package s2.spring.automate.ssm.persister

import com.fasterxml.jackson.databind.ObjectMapper
import f2.dsl.fnc.invoke
import f2.dsl.fnc.invokeWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersister
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

class SsmAutomatePersister<STATE, ID, ENTITY, EVENT>(
	internal var ssmSessionStartFunction: SsmTxSessionStartFunction,
	internal var ssmSessionPerformActionFunction: SsmTxSessionPerformActionFunction,
	internal var dataSsmSessionGetQueryFunction: DataSsmSessionGetQueryFunction,

	internal var chaincodeUri: ChaincodeUri,
	internal var entityType: Class<ENTITY>,
	internal var agentSigner: Agent,
	internal var objectMapper: ObjectMapper,
	internal var permisive: Boolean = false
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun load(automateContext: AutomateContext<S2Automate>, id: ID & Any): ENTITY? {
		val session = getSession(id.toString(), automateContext).item ?: return null
		return objectMapper.readValue(session.state.details.public as String, entityType)
	}

	override suspend fun persist(
		transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
	): ENTITY {
		val entity = transitionContext.entity
		val sessionName = entity.s2Id().toString()
		val iteration = getIteration(GetSessionQuery(transitionContext, sessionName))
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
		return persistInternal(transitionContext)
	}

	private suspend fun persistInternal(
		transitionContext: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
	): ENTITY {
		// Create a list of SsmSessionStartCommands
		val entity = transitionContext.entity
		val automate = transitionContext.automateContext.automate

		val cmd = SsmSessionStartCommand(
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
		// Invoke the ssmSessionStartFunction with all collected commands
		ssmSessionStartFunction.invoke(cmd)

		// Return a flow emitting each event from the collected contexts
		return entity
	}


	private suspend fun getIteration(query: GetSessionQuery<STATE, ID, ENTITY, EVENT>): Int {
		val session = getSession(flowOf(query)).first()
		return session.item?.state?.details?.iteration ?: return 0
	}

	private suspend fun getSession(
		sessionId: SessionName,
		automateContext: AutomateContext<S2Automate>
	) = DataSsmSessionGetQuery(
		sessionName = sessionId,
		ssmUri = chaincodeUri.toSsmUri(automateContext.automate.name)
	).invokeWith(dataSsmSessionGetQueryFunction)


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

}
