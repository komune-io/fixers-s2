package s2.spring.sourcing.ssm

import f2.dsl.fnc.invoke
import f2.dsl.fnc.invokeWith
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository
import ssm.chaincode.dsl.model.Agent
import ssm.chaincode.dsl.model.SessionName
import ssm.chaincode.dsl.model.SsmContext
import ssm.chaincode.dsl.model.SsmSession
import ssm.chaincode.dsl.model.uri.ChaincodeUri
import ssm.chaincode.dsl.model.uri.toSsmUri
import ssm.data.dsl.features.query.DataSsmSessionGetQuery
import ssm.data.dsl.features.query.DataSsmSessionGetQueryFunction
import ssm.data.dsl.features.query.DataSsmSessionListQuery
import ssm.data.dsl.features.query.DataSsmSessionListQueryFunction
import ssm.data.dsl.features.query.DataSsmSessionLogListQuery
import ssm.data.dsl.features.query.DataSsmSessionLogListQueryFunction
import ssm.data.dsl.model.DataSsmSessionStateDTO
import ssm.tx.dsl.features.ssm.SsmSessionPerformActionCommand
import ssm.tx.dsl.features.ssm.SsmSessionPerformActionResult
import ssm.tx.dsl.features.ssm.SsmSessionStartCommand
import ssm.tx.dsl.features.ssm.SsmSessionStartResult
import ssm.tx.dsl.features.ssm.SsmTxSessionPerformActionFunction
import ssm.tx.dsl.features.ssm.SsmTxSessionStartFunction

class EventPersisterSsm<EVENT, ID>(
	private val s2Automate: S2Automate,
	private val eventType: KClass<EVENT>
) : EventRepository<EVENT, ID> where
EVENT: Evt,
EVENT: WithS2Id<ID>
{

	internal lateinit var ssmSessionStartFunction: SsmTxSessionStartFunction
	internal lateinit var ssmSessionPerformActionFunction: SsmTxSessionPerformActionFunction

	internal lateinit var dataSsmSessionGetQueryFunction: DataSsmSessionGetQueryFunction
	internal lateinit var dataSsmSessionLogFunction: DataSsmSessionLogListQueryFunction
	internal lateinit var dataSsmSessionListQueryFunction: DataSsmSessionListQueryFunction


	internal lateinit var chaincodeUri: ChaincodeUri
	internal lateinit var agentSigner: Agent
	internal lateinit var json: Json
	internal var versioning: Boolean = false

	override suspend fun load(id: ID): Flow<EVENT> {
		return getSessionLog(id.toString())
			.items
			.toEvents()
	}

	override suspend fun loadAll(): Flow<EVENT> {
		return listSessions()
			.items
				.flatMap { session ->
					getSessionLog(session.sessionName).items
				}.sortedBy {
					it.transaction?.timestamp
				}
			.toEvents()
	}

	override suspend fun persist(events: Flow<EVENT>): Flow<EVENT> = flow {
		val toCreate = mutableListOf<EVENT>()
		val toUpdate = mutableListOf<EVENT>()
		val processedIds = mutableSetOf<ID & Any>()

		events.collect { event ->
			val eventId = event.s2Id()
			require(eventId !in processedIds){
				"Duplicate ID detected: $eventId. Multiple events with the same ID cannot be processed due to SSM limitations."
			}
			processedIds.add(eventId)

			val sessionName = buildSessionName(event)
			val iteration = getIteration(sessionName)
			if (iteration == null) {
				toCreate.add(event)
			} else {
				toUpdate.add(event)
			}
		}
		if(toCreate.isNotEmpty()) {
			toCreate.asFlow().initFlow().collect()
		}
		if(toUpdate.isNotEmpty()) {
			toUpdate.asFlow().updateFlow().collect()
		}
		emitAll(toCreate.asFlow())
		emitAll(toUpdate.asFlow())
	}

	override suspend fun createTable() {}

	override suspend fun persist(event: EVENT): EVENT {
		val sessionName = buildSessionName(event)
		val iteration = getIteration(sessionName)
		val action = event::class.simpleName!!
		if(iteration == null) {
			init(event)
		} else {
			@OptIn(InternalSerializationApi::class)
			val context = SsmSessionPerformActionCommand(
				action = action,
				context = SsmContext(
					session = sessionName,
					public = json.encodeToString(eventType.serializer(), event),
					private = mapOf(),
					iteration = iteration,
				),
				signerName = agentSigner.name,
				chaincodeUri = chaincodeUri
			)
			ssmSessionPerformActionFunction.invoke(context)
		}
		return event
	}

	private suspend fun Flow<EVENT>.initFlow( ): Flow<SsmSessionStartResult> = map { event ->
		@OptIn(InternalSerializationApi::class)
		SsmSessionStartCommand(
			session = SsmSession(
				ssm = s2Automate.name,
				session = buildSessionName(event),
				roles = mapOf(agentSigner.name to s2Automate.transitions[0].role.name),
				public = json.encodeToString(eventType.serializer(), event),
				private = mapOf()
			),
			signerName = agentSigner.name,
			chaincodeUri = chaincodeUri
		)
	}.let {
		ssmSessionStartFunction.invoke(it)
	}

	private suspend fun Flow<EVENT>.updateFlow(): Flow<SsmSessionPerformActionResult> = map { event ->
		val sessionName = buildSessionName(event)
		val iteration = getIteration(sessionName)!!
		val action = event::class.simpleName!!
		@OptIn(InternalSerializationApi::class)
		SsmSessionPerformActionCommand(
			action = action,
			context = SsmContext(
				session = sessionName,
				public = json.encodeToString(eventType.serializer(), event),
				private = mapOf(),
				iteration = iteration,
			),
			signerName = agentSigner.name,
			chaincodeUri = chaincodeUri
		)
	}.let { toUpdated ->
		ssmSessionPerformActionFunction.invoke(toUpdated)
	}

	private suspend fun init(event: EVENT): EVENT {
		@OptIn(InternalSerializationApi::class)
		val ssmStart = SsmSessionStartCommand(
			session = SsmSession(
				ssm = s2Automate.name,
				session = buildSessionName(event),
				roles = mapOf(agentSigner.name to s2Automate.transitions[0].role.name),
				public = json.encodeToString(eventType.serializer(), event),
				private = mapOf()
			),
			signerName = agentSigner.name,
			chaincodeUri = chaincodeUri
		)
		ssmSessionStartFunction.invoke(ssmStart)
		return event
	}

	private fun buildSessionName(event: EVENT): String {
		return if(versioning) {
			return "${s2Automate.name}-${event.s2Id().toString()}"
		} else {
			event.s2Id().toString()
		}
	}

	private suspend fun getIteration(sessionId: SessionName): Int? {
		return getSession(sessionId)
			.item?.state?.details?.iteration
	}

	private suspend fun getSession(
		sessionId: SessionName,
	) = DataSsmSessionGetQuery(
		sessionName = sessionId,
		ssmUri = chaincodeUri.toSsmUri(s2Automate.name)
	).invokeWith(dataSsmSessionGetQueryFunction)

	private suspend fun getSessionLog(
		sessionId: SessionName,
	) = DataSsmSessionLogListQuery(
		sessionName = sessionId,
		ssmUri = chaincodeUri.toSsmUri(s2Automate.name)
	).invokeWith(dataSsmSessionLogFunction)

	private suspend fun listSessions() = DataSsmSessionListQuery(
		ssmUri = chaincodeUri.toSsmUri(s2Automate.name)
	).invokeWith(dataSsmSessionListQueryFunction)

	@OptIn(InternalSerializationApi::class)
	private fun List<DataSsmSessionStateDTO>.toEvents() = sortedBy { it.details.iteration }.map {
		json.decodeFromString(eventType.serializer(), it.details.public as String)
	}.asFlow()
}
