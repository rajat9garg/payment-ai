# Task 5: Payment Modes API Implementation Plan - Part 2

## 4. Repository Layer Implementation

### 4.1 Create Repository Interfaces

Define the repository interfaces for payment modes:

```kotlin
// src/main/kotlin/com/payment/repositories/PaymentModeRepository.kt
package com.payment.repositories

import com.payment.models.domain.PaymentMode

interface PaymentModeRepository {
    fun findAll(): List<PaymentMode>
    fun findById(id: Long): PaymentMode?
    fun findByModeCode(modeCode: String): PaymentMode?
    fun findActivePaymentModes(): List<PaymentMode>
}
```

```kotlin
// src/main/kotlin/com/payment/repositories/PaymentTypeRepository.kt
package com.payment.repositories

import com.payment.models.domain.PaymentType

interface PaymentTypeRepository {
    fun findAll(): List<PaymentType>
    fun findById(id: Long): PaymentType?
    fun findByModeId(modeId: Long): List<PaymentType>
    fun findByModeIdAndTypeCode(modeId: Long, typeCode: String): PaymentType?
}
```

```kotlin
// src/main/kotlin/com/payment/repositories/ProductPaymentModeRepository.kt
package com.payment.repositories

import com.payment.models.domain.ProductPaymentMode

interface ProductPaymentModeRepository {
    fun findByProductType(productType: String): List<ProductPaymentMode>
    fun findByProductTypeAndModeId(productType: String, modeId: Long): List<ProductPaymentMode>
}
```

```kotlin
// src/main/kotlin/com/payment/repositories/UserPaymentPreferenceRepository.kt
package com.payment.repositories

import com.payment.models.domain.UserPaymentPreference

interface UserPaymentPreferenceRepository {
    fun findByUserId(userId: String): List<UserPaymentPreference>
    fun findBlockedByUserId(userId: String): List<UserPaymentPreference>
    fun findPreferredByUserId(userId: String): List<UserPaymentPreference>
}
```

### 4.2 Implement Repositories Using jOOQ

Implement the repository interfaces using jOOQ:

```kotlin
// src/main/kotlin/com/payment/repositories/impl/PaymentModeRepositoryImpl.kt
package com.payment.repositories.impl

import com.payment.jooq.tables.references.PAYMENT_MODES
import com.payment.mappers.PaymentModeMapper
import com.payment.models.domain.PaymentMode
import com.payment.repositories.PaymentModeRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PaymentModeRepositoryImpl(private val dsl: DSLContext) : PaymentModeRepository {
    
    override fun findAll(): List<PaymentMode> {
        return dsl.selectFrom(PAYMENT_MODES)
            .fetch()
            .map { PaymentModeMapper.toDomain(it) }
    }
    
    override fun findById(id: Long): PaymentMode? {
        return dsl.selectFrom(PAYMENT_MODES)
            .where(PAYMENT_MODES.ID.eq(id))
            .fetchOne()
            ?.let { PaymentModeMapper.toDomain(it) }
    }
    
    override fun findByModeCode(modeCode: String): PaymentMode? {
        return dsl.selectFrom(PAYMENT_MODES)
            .where(PAYMENT_MODES.MODE_CODE.eq(modeCode))
            .fetchOne()
            ?.let { PaymentModeMapper.toDomain(it) }
    }
    
    override fun findActivePaymentModes(): List<PaymentMode> {
        return dsl.selectFrom(PAYMENT_MODES)
            .where(PAYMENT_MODES.IS_ACTIVE.eq(true))
            .fetch()
            .map { PaymentModeMapper.toDomain(it) }
    }
}
```

```kotlin
// src/main/kotlin/com/payment/repositories/impl/PaymentTypeRepositoryImpl.kt
package com.payment.repositories.impl

import com.payment.jooq.tables.references.PAYMENT_TYPES
import com.payment.mappers.PaymentTypeMapper
import com.payment.models.domain.PaymentType
import com.payment.repositories.PaymentTypeRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PaymentTypeRepositoryImpl(private val dsl: DSLContext) : PaymentTypeRepository {
    
    override fun findAll(): List<PaymentType> {
        return dsl.selectFrom(PAYMENT_TYPES)
            .fetch()
            .map { PaymentTypeMapper.toDomain(it) }
    }
    
    override fun findById(id: Long): PaymentType? {
        return dsl.selectFrom(PAYMENT_TYPES)
            .where(PAYMENT_TYPES.ID.eq(id))
            .fetchOne()
            ?.let { PaymentTypeMapper.toDomain(it) }
    }
    
    override fun findByModeId(modeId: Long): List<PaymentType> {
        return dsl.selectFrom(PAYMENT_TYPES)
            .where(PAYMENT_TYPES.MODE_ID.eq(modeId))
            .fetch()
            .map { PaymentTypeMapper.toDomain(it) }
    }
    
    override fun findByModeIdAndTypeCode(modeId: Long, typeCode: String): PaymentType? {
        return dsl.selectFrom(PAYMENT_TYPES)
            .where(PAYMENT_TYPES.MODE_ID.eq(modeId))
            .and(PAYMENT_TYPES.TYPE_CODE.eq(typeCode))
            .fetchOne()
            ?.let { PaymentTypeMapper.toDomain(it) }
    }
}
```

```kotlin
// src/main/kotlin/com/payment/repositories/impl/ProductPaymentModeRepositoryImpl.kt
package com.payment.repositories.impl

import com.payment.jooq.tables.references.PRODUCT_PAYMENT_MODES
import com.payment.models.domain.ProductPaymentMode
import com.payment.repositories.ProductPaymentModeRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProductPaymentModeRepositoryImpl(private val dsl: DSLContext) : ProductPaymentModeRepository {
    
    override fun findByProductType(productType: String): List<ProductPaymentMode> {
        return dsl.selectFrom(PRODUCT_PAYMENT_MODES)
            .where(PRODUCT_PAYMENT_MODES.PRODUCT_TYPE.eq(productType))
            .and(PRODUCT_PAYMENT_MODES.IS_ACTIVE.eq(true))
            .fetch()
            .map { record ->
                ProductPaymentMode(
                    id = record.id,
                    productType = record.productType,
                    modeId = record.modeId,
                    typeId = record.typeId,
                    isActive = record.isActive ?: true,
                    createdAt = record.createdAt ?: LocalDateTime.now(),
                    updatedAt = record.updatedAt ?: LocalDateTime.now()
                )
            }
    }
    
    override fun findByProductTypeAndModeId(productType: String, modeId: Long): List<ProductPaymentMode> {
        return dsl.selectFrom(PRODUCT_PAYMENT_MODES)
            .where(PRODUCT_PAYMENT_MODES.PRODUCT_TYPE.eq(productType))
            .and(PRODUCT_PAYMENT_MODES.MODE_ID.eq(modeId))
            .and(PRODUCT_PAYMENT_MODES.IS_ACTIVE.eq(true))
            .fetch()
            .map { record ->
                ProductPaymentMode(
                    id = record.id,
                    productType = record.productType,
                    modeId = record.modeId,
                    typeId = record.typeId,
                    isActive = record.isActive ?: true,
                    createdAt = record.createdAt ?: LocalDateTime.now(),
                    updatedAt = record.updatedAt ?: LocalDateTime.now()
                )
            }
    }
}
```

```kotlin
// src/main/kotlin/com/payment/repositories/impl/UserPaymentPreferenceRepositoryImpl.kt
package com.payment.repositories.impl

import com.payment.jooq.tables.references.USER_PAYMENT_PREFERENCES
import com.payment.models.domain.UserPaymentPreference
import com.payment.repositories.UserPaymentPreferenceRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UserPaymentPreferenceRepositoryImpl(private val dsl: DSLContext) : UserPaymentPreferenceRepository {
    
    override fun findByUserId(userId: String): List<UserPaymentPreference> {
        return dsl.selectFrom(USER_PAYMENT_PREFERENCES)
            .where(USER_PAYMENT_PREFERENCES.USER_ID.eq(userId))
            .fetch()
            .map { record ->
                UserPaymentPreference(
                    id = record.id,
                    userId = record.userId,
                    modeId = record.modeId,
                    typeId = record.typeId,
                    isPreferred = record.isPreferred ?: false,
                    isBlocked = record.isBlocked ?: false,
                    createdAt = record.createdAt ?: LocalDateTime.now(),
                    updatedAt = record.updatedAt ?: LocalDateTime.now()
                )
            }
    }
    
    override fun findBlockedByUserId(userId: String): List<UserPaymentPreference> {
        return dsl.selectFrom(USER_PAYMENT_PREFERENCES)
            .where(USER_PAYMENT_PREFERENCES.USER_ID.eq(userId))
            .and(USER_PAYMENT_PREFERENCES.IS_BLOCKED.eq(true))
            .fetch()
            .map { record ->
                UserPaymentPreference(
                    id = record.id,
                    userId = record.userId,
                    modeId = record.modeId,
                    typeId = record.typeId,
                    isPreferred = record.isPreferred ?: false,
                    isBlocked = record.isBlocked ?: true,
                    createdAt = record.createdAt ?: LocalDateTime.now(),
                    updatedAt = record.updatedAt ?: LocalDateTime.now()
                )
            }
    }
    
    override fun findPreferredByUserId(userId: String): List<UserPaymentPreference> {
        return dsl.selectFrom(USER_PAYMENT_PREFERENCES)
            .where(USER_PAYMENT_PREFERENCES.USER_ID.eq(userId))
            .and(USER_PAYMENT_PREFERENCES.IS_PREFERRED.eq(true))
            .fetch()
            .map { record ->
                UserPaymentPreference(
                    id = record.id,
                    userId = record.userId,
                    modeId = record.modeId,
                    typeId = record.typeId,
                    isPreferred = record.isPreferred ?: true,
                    isBlocked = record.isBlocked ?: false,
                    createdAt = record.createdAt ?: LocalDateTime.now(),
                    updatedAt = record.updatedAt ?: LocalDateTime.now()
                )
            }
    }
}
```

## 5. Service Layer Implementation

### 5.1 Create Payment Modes Service Interface

Define the payment modes service interface:

```kotlin
// src/main/kotlin/com/payment/services/PaymentModesService.kt
package com.payment.services

interface PaymentModesService {
    /**
     * Get available payment modes for a user and product type
     * 
     * @param userId The ID of the user
     * @param productType The product type
     * @return Map of payment modes and their types
     */
    fun getAvailablePaymentModes(userId: String, productType: String): Map<String, Any>
}
```

### 5.2 Implement Payment Modes Service

Implement the payment modes service:

```kotlin
// src/main/kotlin/com/payment/services/impl/PaymentModesServiceImpl.kt
package com.payment.services.impl

import com.payment.models.domain.PaymentMode
import com.payment.models.domain.PaymentType
import com.payment.repositories.PaymentModeRepository
import com.payment.repositories.PaymentTypeRepository
import com.payment.repositories.ProductPaymentModeRepository
import com.payment.repositories.UserPaymentPreferenceRepository
import com.payment.services.PaymentModesService
import org.springframework.stereotype.Service

@Service
class PaymentModesServiceImpl(
    private val paymentModeRepository: PaymentModeRepository,
    private val paymentTypeRepository: PaymentTypeRepository,
    private val productPaymentModeRepository: ProductPaymentModeRepository,
    private val userPaymentPreferenceRepository: UserPaymentPreferenceRepository
) : PaymentModesService {

    /**
     * Get available payment modes for a user and product type
     * Algorithm:
     * 1. Get all payment modes supported for the product type
     * 2. Get user preferences (blocked and preferred payment modes)
     * 3. Filter out blocked payment modes
     * 4. Organize payment modes and types into a structured response
     * 5. Sort payment modes with preferred modes first
     * 
     * @param userId The ID of the user
     * @param productType The product type
     * @return Map of payment modes and their types
     */
    override fun getAvailablePaymentModes(userId: String, productType: String): Map<String, Any> {
        // Step 1: Get all product payment modes for the product type
        val productPaymentModes = productPaymentModeRepository.findByProductType(productType)
        
        if (productPaymentModes.isEmpty()) {
            return mapOf("paymentModes" to emptyList<Map<String, Any>>())
        }
        
        // Get unique mode IDs from product payment modes
        val modeIds = productPaymentModes.map { it.modeId }.distinct()
        
        // Get all payment modes for these IDs
        val paymentModes = modeIds.mapNotNull { paymentModeRepository.findById(it) }
            .filter { it.isActive }
        
        // Step 2: Get user preferences
        val userPreferences = userPaymentPreferenceRepository.findByUserId(userId)
        val blockedPreferences = userPaymentPreferenceRepository.findBlockedByUserId(userId)
        val preferredPreferences = userPaymentPreferenceRepository.findPreferredByUserId(userId)
        
        // Create sets of blocked mode IDs and type IDs
        val blockedModeIds = blockedPreferences
            .filter { it.typeId == null }
            .map { it.modeId }
            .toSet()
            
        val blockedTypeIds = blockedPreferences
            .filter { it.typeId != null }
            .map { it.typeId!! }
            .toSet()
        
        // Create sets of preferred mode IDs and type IDs
        val preferredModeIds = preferredPreferences
            .filter { it.typeId == null }
            .map { it.modeId }
            .toSet()
            
        val preferredTypeIds = preferredPreferences
            .filter { it.typeId != null }
            .map { it.typeId!! }
            .toSet()
        
        // Step 3: Filter out blocked payment modes
        val filteredPaymentModes = paymentModes
            .filter { !blockedModeIds.contains(it.id) }
        
        // Step 4: Organize payment modes and types into a structured response
        val result = filteredPaymentModes.map { mode ->
            // Get all payment types for this mode
            val allTypes = paymentTypeRepository.findByModeId(mode.id!!)
                .filter { it.isActive }
            
            // Filter out types not supported for this product
            val supportedTypeIds = productPaymentModes
                .filter { it.modeId == mode.id && it.typeId != null }
                .map { it.typeId!! }
                .toSet()
                
            val types = allTypes
                .filter { supportedTypeIds.contains(it.id) }
                .filter { !blockedTypeIds.contains(it.id) }
                .map { type ->
                    mapOf(
                        "code" to type.typeCode,
                        "name" to type.typeName,
                        "description" to (type.description ?: ""),
                        "isPreferred" to preferredTypeIds.contains(type.id)
                    )
                }
                .sortedByDescending { it["isPreferred"] as Boolean }
            
            mapOf(
                "code" to mode.modeCode,
                "name" to mode.modeName,
                "description" to (mode.description ?: ""),
                "isPreferred" to preferredModeIds.contains(mode.id),
                "types" to types
            )
        }
        
        // Step 5: Sort payment modes with preferred modes first
        val sortedResult = result.sortedWith(
            compareByDescending<Map<String, Any>> { it["isPreferred"] as Boolean }
                .thenBy { it["name"] as String }
        )
        
        return mapOf("paymentModes" to sortedResult)
    }
}
```
