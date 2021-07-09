package com.ubertob.kondor.outcome

typealias UnitOutcome =Outcome<OutcomeError, Unit>

typealias BaseOutcome<T> = Outcome<OutcomeError, T>