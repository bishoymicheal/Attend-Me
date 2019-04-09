package net.corpy.loginlocation.model

import androidx.annotation.IntDef

const val ADMIN = 2
const val MANAGER = 1
const val NORMAL = 0

@IntDef(ADMIN, MANAGER, NORMAL)
annotation class EmployeesTypes