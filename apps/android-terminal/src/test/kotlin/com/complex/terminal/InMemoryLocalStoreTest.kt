package com.complex.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InMemoryLocalStoreTest {
  @Test
  fun clearSensitiveDataClearsSessionAndBinding() {
    val store = InMemoryLocalStore()
    store.saveSession(TerminalSession("token", "staff-1", "admin160"))
    store.saveBinding(DeviceBinding(id = "device-1", deviceCode = "D-1", status = "bound"))

    assertNotNull(store.session)
    store.clearSensitiveData()

    assertEquals(null, store.session)
    assertEquals(null, store.binding)
  }
}
