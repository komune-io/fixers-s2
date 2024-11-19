package s2.spring.automate.ssm.persister

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersisterFlow
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import ssm.chaincode.dsl.model.Agent
import ssm.chaincode.dsl.model.SessionName
import ssm.chaincode.dsl.model.SsmContext
import ssm.chaincode.dsl.model.SsmSession
import ssm.chaincode.dsl.model.uri.ChaincodeUri
import ssm.chaincode.dsl.model.uri.toSsmUri
import ssm.data.dsl.features.query.DataSsmSessionGetQuery
import ssm.data.dsl.features.query.DataSsmSessionGetQueryFunction
import ssm.tx.dsl.features.ssm.SsmSessionPerformActionCommand
import ssm.tx.dsl.features.ssm.SsmSessionStartCommand
import ssm.tx.dsl.features.ssm.SsmTxSessionPerformActionFunction
import ssm.tx.dsl.features.ssm.SsmTxSessionStartFunction

class SsmAutomatePersisterFlow<STATE, ID, ENTITY, EVENT>(
	internal var ssmSessionStartFunction: SsmTxSessionStartFunction,
	internal var ssmSessionPerformActionFunction: SsmTxSessionPerformActionFunction,
	internal var dataSsmSessionGetQueryFunction: DataSsmSessionGetQueryFunction,

	internal var chaincodeUri: ChaincodeUri,
	internal var entityType: Class<ENTITY>,
	internal var agentSigner: Agent,
	internal var objectMapper: ObjectMapper,
	internal var permisive: Boolean = false
) : AutomatePersisterFlow<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	private val logger = LoggerFactory.getLogger(SsmAutomatePersister::class.java)

	override suspend fun load(automateContexts: AutomateContext<S2Automate>, id: ID & Any): ENTITY? {
		return load(automateContexts, flowOf(id)).firstOrNull()
	}

	override suspend fun load(automateContexts: AutomateContext<S2Automate>, ids: Flow<ID & Any>): Flow<ENTITY> {
		return ids.map {
			GetAutomateSessionQuery(automateContext = automateContexts, sessionId = it.toString())
		}.let {
			getSessionForAutomate(it)
		}.map { session ->
			objectMapper.readValue(session.item!!.state.details.public as String, entityType)
		}

	}

	override suspend fun persistInitFlow(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return persistInternal(transitionContexts).map { it.second }
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

	private suspend fun getIterations(
		query: Flow<GetSessionQuery<STATE, ID, ENTITY, EVENT>>
	): Flow<GetSessionResult<STATE, ID, ENTITY, EVENT>> {
		val list = query.toList()
		logger.info("//////////////////////")
		logger.info("//////////////////////")
		logger.info("//////////////////////")
		logger.info("Get iterations for ${list.size} sessions")
		val bySession = list.associateBy { it.sessionId }

		return getSession(list.asFlow()).map { session ->
			val it = bySession[session.item?.sessionName]!!
			session.item?.state?.details?.iteration ?: 0

			GetSessionResult(
				transitionContext = it.transitionContext,
				sessionId = it.sessionId,
				iteration = session.item?.state?.details?.iteration ?: 0
			)
		}

	}

	private suspend fun getSession(
		queries: Flow<GetSessionQuery<STATE, ID, ENTITY, EVENT>>,
	) = queries.map { query ->
		DataSsmSessionGetQuery(
			sessionName = query.sessionId,
			ssmUri = chaincodeUri.toSsmUri(query.transitionContext.automateContext.automate.name)
		)
	}.let {
		dataSsmSessionGetQueryFunction.invoke(it)
	}

	private suspend fun getSessionForAutomate(
		queries: Flow<GetAutomateSessionQuery>,
	) = queries.map { query ->
		DataSsmSessionGetQuery(
			sessionName = query.sessionId,
			ssmUri = chaincodeUri.toSsmUri(query.automateContext.automate.name)
		)
	}.let {
		dataSsmSessionGetQueryFunction.invoke(it)
	}

	override suspend fun persistFlow(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> = flow {
		val collectedContexts = transitionContexts.toList()

		val ssmCommands = collectedContexts.map { transitionContext ->
			val sessionName = transitionContext.entity.s2Id().toString()
			GetSessionQuery(transitionContext, sessionName)
		}.let {
			getIterations(it.asFlow())
		}.map { (transitionContext, _,iteration) ->

			val entity = transitionContext.entity

			val withEventAsAction = transitionContext.automateContext.automate.withResultAsAction
			val action = transitionContext.event?.takeIf { withEventAsAction } ?: transitionContext.msg
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
		}

		ssmSessionPerformActionFunction.invoke(ssmCommands).collect()

		collectedContexts.forEach { e ->
			emit(e.event)
		}
	}

}

data class GetSessionQuery<STATE, ID, ENTITY, EVENT>(
	val transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
	val sessionId: SessionName
) where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>

data class GetAutomateSessionQuery(
	val automateContext: AutomateContext<S2Automate>,
	val sessionId: SessionName
)

data class GetSessionResult<STATE, ID, ENTITY, EVENT>(
	val transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
	val sessionId: SessionName,
	val iteration: Int
) where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>
