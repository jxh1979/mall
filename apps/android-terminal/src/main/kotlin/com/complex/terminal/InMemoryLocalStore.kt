package com.complex.terminal

import java.time.Instant

class InMemoryLocalStore {
  var session: TerminalSession? = null
    private set

  var binding: DeviceBinding? = null
    private set

  private val events = mutableListOf<TerminalEvent>()

  fun saveSession(value: TerminalSession) {
    session = value
  }

  fun saveBinding(value: DeviceBinding) {
    binding = value
  }

  fun addEvent(event: TerminalEvent) {
    events += event
  }

  fun pendingEvents(): List<TerminalEvent> = events.toList()

  fun clearEvents() {
    events.clear()
  }

  fun clearSensitiveData() {
    session = null
    binding = null
    events.clear()
  }
}

fun nowIso(): String = Instant.now().toString()
