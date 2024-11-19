package s2.sourcing.dsl

import f2.dsl.fnc.F2Function
import s2.dsl.automate.Evt

fun interface Evolve<EVENT, ENTITY> : F2Function<EVENT, ENTITY> where
EVENT : Evt
