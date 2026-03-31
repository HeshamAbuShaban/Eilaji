package com.eilaji.backend.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp as jdaTimestamp
import java.time.Instant

object Medicines : Table("medicines") {
    val id = uuid("id").autoGenerate()
    val titleAr = varchar("title_ar", 255)
    val titleEn = varchar("title_en", 255)
    val descriptionAr = text("description_ar").nullable()
    val descriptionEn = text("description_en").nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val price = decimal("price", 10, 2).nullable()
    val subcategoryId = reference("subcategory_id", Subcategories.id).onDeleteSetNull().nullable()
    val manufacturer = varchar("manufacturer", 255).nullable()
    val requiresPrescription = bool("requires_prescription").default(false)
    val alternativesIds = array("alternatives_ids", "uuid").defaultValue(emptyList())
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }
    
    override val primaryKey = PrimaryKey(id)
}

object Categories : Table("categories") {
    val id = uuid("id").autoGenerate()
    val nameAr = varchar("name_ar", 255)
    val nameEn = varchar("name_en", 255)
    val iconUrl = varchar("icon_url", 500).nullable()
    val parentId = reference("parent_id", id).onDeleteSetNull().nullable()
    val displayOrder = integer("display_order").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    
    override val primaryKey = PrimaryKey(id)
}

object Subcategories : Table("subcategories") {
    val id = uuid("id").autoGenerate()
    val categoryId = reference("category_id", Categories.id).onDeleteCascade()
    val nameAr = varchar("name_ar", 255)
    val nameEn = varchar("name_en", 255)
    val iconUrl = varchar("icon_url", 500).nullable()
    val displayOrder = integer("display_order").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    
    override val primaryKey = PrimaryKey(id)
}
