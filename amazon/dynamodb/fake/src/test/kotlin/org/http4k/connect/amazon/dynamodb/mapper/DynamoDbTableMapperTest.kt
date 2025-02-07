package org.http4k.connect.amazon.dynamodb.mapper

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.model.*
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import java.util.UUID

private val ownerIdAttr = Attribute.uuid().required("ownerId")
private val nameAttr = Attribute.string().required("name")
private val bornAttr = Attribute.localDate().required("born")
private val idAttr = Attribute.uuid().required("id")

private val byOwner = DynamoDbTableMapperSchema.GlobalSecondary(
    indexName = IndexName.of("by-owner"),
    hashKeyAttribute = ownerIdAttr,
    sortKeyAttribute = nameAttr
)

private val byDob = DynamoDbTableMapperSchema.GlobalSecondary(
    indexName = IndexName.of("by-dob"),
    hashKeyAttribute = bornAttr,
    sortKeyAttribute = idAttr
)

class DynamoDbTableMapperTest {

    private val storage: Storage<DynamoTable> = Storage.InMemory()
    private val tableMapper = FakeDynamoDb(storage).client().tableMapper<Cat, UUID, Unit>(
        TableName = TableName.of("cats"),
        hashKeyAttribute = idAttr
    )

    init {
        tableMapper.createTable(byOwner, byDob)
        tableMapper += listOf(toggles, smokie, bandit)
    }

    private fun table() = storage["cats"]!!

    @Test
    fun `verify cats table`() {
        val tableData = table().table

        assertThat(tableData.TableName, equalTo(TableName.of("cats")))
        assertThat(tableData.KeySchema, equalTo(KeySchema.compound(AttributeName.of("id"))))
        assertThat(
            tableData.AttributeDefinitions?.toSet(),
            equalTo(setOf(
                AttributeDefinition(AttributeName.of("id"), DynamoDataType.S),
                AttributeDefinition(AttributeName.of("ownerId"), DynamoDataType.S),
                AttributeDefinition(AttributeName.of("name"), DynamoDataType.S),
                AttributeDefinition(AttributeName.of("born"), DynamoDataType.S)
            ))
        )
        assertThat(tableData.GlobalSecondaryIndexes.orEmpty(), hasSize(equalTo(2)))
        assertThat(tableData.LocalSecondaryIndexes, absent())
    }

    @Test
    fun `scan table`() {
        assertThat(
            tableMapper.primaryIndex().scan().toSet(),
            equalTo(setOf(toggles, smokie, bandit))
        )
    }

    @Test
    fun `get item`() {
        assertThat(tableMapper[toggles.id], equalTo(toggles))
    }

    @Test
    fun `get missing item`() {
        assertThat(tableMapper[UUID.randomUUID()], absent())
    }

    @Test
    fun `query for index`() {
        assertThat(
            tableMapper.index(byOwner).query(owner2).toList(),
            equalTo(listOf(bandit, smokie))
        )
    }

    @Test
    fun `query for index - reverse order`() {
        assertThat(
            tableMapper.index(byOwner).query(owner2, scanIndexForward = false).toList(),
            equalTo(listOf(smokie, bandit))
        )
    }

    @Test
    fun `delete item`() {
        tableMapper -= toggles

        assertThat(table().items, hasSize(equalTo(2)))
    }

    @Test
    fun `delete missing item`() {
        tableMapper.delete(UUID.randomUUID())

        assertThat(table().items, hasSize(equalTo(3)))
    }

    @Test
    fun `delete batch`() {
        tableMapper -= listOf(smokie, bandit)

        assertThat(table().items, hasSize(equalTo(1)))
    }

    @Test
    fun `delete table`() {
        tableMapper.deleteTable().successValue()
        assertThat(storage["cats"], absent())
    }

    @Test
    fun `custom query`() {
        val results = tableMapper.index(byDob).query(
            filter = "$bornAttr = :val1",
            values = mapOf(":val1" to bornAttr.asValue(smokie.born))
        ).toList()

        assertThat(results, equalTo(listOf(smokie, bandit)))
    }
}
