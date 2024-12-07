package s2.spring.core

import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.context.AutomateContext
import s2.automate.core.engine.BatchParams
import s2.automate.core.guard.Guard
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.guard.GuardVerifierImpl
import s2.automate.core.guard.TransitionStateGuard
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.spring.core.publisher.SpringEventPublisher

abstract class S2SpringAdapterBase<ENTITY, STATE, EVENT, ID> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt{

	protected open fun automateContext() = AutomateContext(automate(), batchParams)

	protected open fun guardExecutor(
		automateAppEventPublisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>,
	): GuardVerifier<STATE, ID, ENTITY, EVENT, S2Automate> {
		return GuardVerifierImpl(
			guards = guards(),
			publisher = automateAppEventPublisher
		)
	}

	protected open fun automateAppEventPublisher(eventPublisher: SpringEventPublisher)
			: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate> {
		return AutomateEventPublisher(eventPublisher)
	}

	protected open fun guards(): List<Guard<STATE, ID, ENTITY, EVENT, S2Automate>> = listOf(
		TransitionStateGuard()
	)

	abstract fun automate(): S2Automate
	open var batchParams: BatchParams = BatchParams()

}
