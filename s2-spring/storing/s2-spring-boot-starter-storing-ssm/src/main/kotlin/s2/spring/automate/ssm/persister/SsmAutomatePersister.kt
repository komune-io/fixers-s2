package s2.spring.automate.ssm.persister

import com.fasterxml.jackson.databind.ObjectMapper
import f2.dsl.fnc.invoke
import f2.dsl.fnc.invokeWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.dsl.automate.ssm.toSsm
import ssm.chaincode.dsl.model.Agent
import ssm.chaincode.dsl.model.SessionName
import ssm.chaincode.dsl.model.SsmContext
import ssm.chaincode.dsl.model.SsmSession
import ssm.chaincode.dsl.model.uri.ChaincodeUri
import ssm.chaincode.dsl.model.uri.toSsmUri
import ssm.data.dsl.features.query.DataSsmSessionGetQuery
import ssm.data.dsl.features.query.DataSsmSessionGetQueryFunction
import ssm.tx.dsl.features.ssm.SsmInitCommand
import ssm.tx.dsl.features.ssm.SsmSessionPerformActionCommand
import ssm.tx.dsl.features.ssm.SsmSessionStartCommand
import ssm.tx.dsl.features.ssm.SsmTxInitFunction
import ssm.tx.dsl.features.ssm.SsmTxSessionPerformActionFunction
import ssm.tx.dsl.features.ssm.SsmTxSessionStartFunction

class SsmAutomatePersister<STATE, ID, ENTITY, EVENT> : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	internal lateinit var ssmSessionStartFunction: SsmTxSessionStartFunction
	internal lateinit var ssmSessionPerformActionFunction: SsmTxSessionPerformActionFunction
	internal lateinit var dataSsmSessionGetQueryFunction: DataSsmSessionGetQueryFunction

	internal lateinit var chaincodeUri: ChaincodeUri
	internal lateinit var entityType: Class<ENTITY>
	internal lateinit var agentSigner: Agent
	internal lateinit var objectMapper: ObjectMapper
	internal var permisive: Boolean = false

	override suspend fun load(automateContext: AutomateContext<S2Automate>, id: ID & Any): ENTITY? {
		val session = getSession(id.toString(), automateContext).item ?: return null
		return objectMapper.readValue(session.state.details.public as String, entityType)
	}

	override suspend fun persist(
		transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
	): ENTITY {
		val entity = transitionContext.entity
		val sessionName = entity.s2Id().toString()
		val iteration = getIteration(transitionContext.automateContext, sessionName)
		val context = SsmSessionPerformActionCommand(
			action = transitionContext.msg::class.simpleName!!,
			context = SsmContext(
				session = entity.s2Id().toString(),
				public = objectMapper.writeValueAsString(entity),
				private = mapOf(),
				iteration = iteration,
			),
			signerName = agentSigner.name,
			chaincodeUri = chaincodeUri
		)
		ssmSessionPerformActionFunction.invoke(context)
		return entity
	}

	override suspend fun persist(
		transitionContext: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
	): ENTITY {
		return persistInternal(flowOf(transitionContext)).toList().first().first
	}

	private suspend fun persistInternal(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<Pair<ENTITY, EVENT>> {
		val collectedContexts = transitionContexts.toList()

		// Create a list of SsmSessionStartCommands
		val ssmStartCommands = collectedContexts.map { transitionContext ->
			val entity = transitionContext.entity
			val automate = transitionContext.automateContext.automate

			SsmSessionStartCommand(
				session = SsmSession(
					ssm = automate.name,
					session = entity.s2Id().toString(),
					roles = mapOf(agentSigner.name to automate.transitions[0].role.name),
					public = objectMapper.writeValueAsString(entity),
					private = mapOf()
				),
				signerName = agentSigner.name,
				chaincodeUri = chaincodeUri
			)
		}

		// Invoke the ssmSessionStartFunction with all collected commands
		ssmSessionStartFunction.invoke(ssmStartCommands.asFlow()).collect()

		// Return a flow emitting each event from the collected contexts
		return flow {
			collectedContexts.forEach { transitionContext ->
				emit(transitionContext.entity to transitionContext.event)
			}
		}
	}

	private suspend fun getIteration(automateContext: AutomateContext<S2Automate>, sessionId: SessionName): Int {
		val session = getSession(sessionId, automateContext)
		return session.item?.state?.details?.iteration ?: return 0
	}

	private suspend fun getSession(
		sessionId: SessionName,
		automateContext: AutomateContext<S2Automate>
	) = DataSsmSessionGetQuery(
		sessionName = sessionId,
		ssmUri = chaincodeUri.toSsmUri(automateContext.automate.name)
	).invokeWith(dataSsmSessionGetQueryFunction)

	override suspend fun persistInitFlow(
		transitionContext: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return persistInternal(transitionContext).map { it.second }
	}

	override suspend fun persistFlow(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> = flow {
		val collectedContexts = transitionContexts.toList()
		val ssmCommands = collectedContexts.map { transitionContext ->
			val entity = transitionContext.entity
			val sessionName = entity.s2Id().toString()
			val iteration = getIteration(transitionContext.automateContext, sessionName)
			val action = transitionContext.event ?: transitionContext.msg
			SsmSessionPerformActionCommand(
				action = action::class.simpleName!!,
				context = SsmContext(
					session = entity.s2Id().toString(),
					public = objectMapper.writeValueAsString(entity),
					private = mapOf(),
					iteration = iteration,
				),
				signerName = agentSigner.name,
				chaincodeUri = chaincodeUri
			)
		}.toList()

		ssmSessionPerformActionFunction.invoke(ssmCommands.asFlow()).collect()

		collectedContexts.forEach { e ->
			emit(e.event)
		}
	}


}
