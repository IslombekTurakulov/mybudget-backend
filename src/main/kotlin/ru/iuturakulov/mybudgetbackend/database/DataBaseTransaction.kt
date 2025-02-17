package ru.iuturakulov.mybudgetbackend.database

sealed class DataBaseTransaction {
    data object FOUND : DataBaseTransaction()  // Операция успешно выполнена (например, пользователь найден)
    data object NOT_FOUND : DataBaseTransaction()  // Данные не найдены (например, email не зарегистрирован)
    data object UPDATED : DataBaseTransaction()  // Данные успешно обновлены
    data object FAILED : DataBaseTransaction()  // Ошибка обновления или удаления данных
}
