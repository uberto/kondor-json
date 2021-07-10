package com.ubertob.kondor.outcome

typealias UnitOutcome = BaseOutcome<Unit>

typealias BaseOutcome<T> = Outcome<OutcomeError, T>