package ru.iuturakulov.mybudgetbackend.repositories.interfaces

interface IEntityRepository<TEntity>{
    suspend fun getAll() : List<TEntity>
    suspend fun findById(id: String) : TEntity?
    suspend fun find(predicate: (TEntity) -> Boolean) : TEntity?
    suspend fun insert(entity : TEntity)
    suspend fun delete(id: String)
    suspend fun replace(item : TEntity)
}