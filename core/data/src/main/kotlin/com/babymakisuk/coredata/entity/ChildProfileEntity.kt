package com.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babymakisuk.coremodel.*
import java.time.LocalDate

@Entity(tableName = "child_profile")
data class ChildProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gender: String,
    val birthday: LocalDate,
    val bloodType: String? = null,
    val note: String = ""
)

fun ChildProfileEntity.toDomain() = ChildProfile(
    id = id, name = name,
    gender = Gender.valueOf(gender),
    birthday = birthday,
    bloodType = bloodType?.let { BloodType.valueOf(it) },
    note = note
)

fun ChildProfile.toEntity() = ChildProfileEntity(
    id = id, name = name,
    gender = gender.name,
    birthday = birthday,
    bloodType = bloodType?.name,
    note = note
)
