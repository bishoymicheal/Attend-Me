package net.corpy.loginlocation.model

data class Employee(
    var email: String = "",
    var phone: String = "",
    var fullName: String = "",
    var gender: Int = -1,
    var description: String = "",
    var birthDate: String = "",
    var password: String = "",
    @EmployeesTypes var type: Int = NORMAL,
    var id: String = "",
    var image :String = ""
)
