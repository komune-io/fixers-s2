package s2.spring.automate.ssm.persister

import com.fasterxml.jackson.databind.ObjectMapper
import f2.dsl.fnc.invoke
import f2.dsl.fnc.invokeWith
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

class SsmAutomatePersister<STATE, ID, ENTITY> : AutomatePersister<STATE, ID, ENTITY, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	internal lateinit var ssmTxInitFunction: SsmTxInitFunction
	internal lateinit var ssmSessionStartFunction: SsmTxSessionStartFunction
	internal lateinit var ssmSessionPerformActionFunction: SsmTxSessionPerformActionFunction
	internal lateinit var dataSsmSessionGetQueryFunction: DataSsmSessionGetQueryFunction

	internal lateinit var chaincodeUri: ChaincodeUri
	internal lateinit var entityType: Class<ENTITY>
	internal lateinit var agentSigner: Agent
	internal lateinit var objectMapper: ObjectMapper



	override suspend fun persist(
		transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, S2Automate>,
	): ENTITY {
		val entity = transitionContext.entity
		val sessionName = entity.s2Id().toString()
		val iteration = getIteration(transitionContext.automateContext, sessionName)
		ssmTxInitFunction.invoke(SsmInitCommand(
			signerName = agentSigner.name,
			ssm = transitionContext.automateContext.automate.toSsm(),
			agent = agentSigner
		))
		val context = SsmSessionPerformActionCommand(
			action = transitionContext.msg::class.simpleName!!,
			context = SsmContext(
				session = entity.s2Id().toString(),
				public = objectMapper.writeValueAsString(entity),
				private = mapOf(),
				iteration = iteration,
			),
			signerName = agentSigner.name
		)
		ssmSessionPerformActionFunction.invoke(context)
		return entity
	}

	override suspend fun load(automateContext: AutomateContext<S2Automate>, id: ID): ENTITY? {
		val session = getSession( id.toString(), automateContext).item ?: return null
		return objectMapper.readValue(session.state.details.public as String, entityType)
	}

	override suspend fun persist(transitionContext: InitTransitionAppliedContext<STATE, ID, ENTITY, S2Automate>): ENTITY {
		val entity = transitionContext.entity
		val automate = transitionContext.automateContext.automate

		val ssmStart = SsmSessionStartCommand(
			session = SsmSession(
				ssm = automate.name,
				session = entity.s2Id().toString(),
				roles = mapOf(agentSigner.name to automate.transitions.get(0).role::class.simpleName!!),
				public = objectMapper.writeValueAsString(entity),
				private = mapOf()
			),
			signerName = agentSigner.name
		)
		ssmSessionStartFunction.invoke(ssmStart)
		return entity
	}

	private suspend fun getIteration(automateContext: AutomateContext<S2Automate>, sessionId: SessionName): Int {
		return getSession(sessionId, automateContext)
			.item?.state?.details?.iteration ?: return 0
	}

	private suspend fun getSession(
		sessionId: SessionName,
		automateContext: AutomateContext<S2Automate>
	) = DataSsmSessionGetQuery(
		sessionName = sessionId,
		ssmUri = chaincodeUri.toSsmUri(automateContext.automate.name)
	).invokeWith(dataSsmSessionGetQueryFunction)
}
