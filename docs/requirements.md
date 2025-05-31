This repository contains all the functions of a working payment gateway

Requirements:
1. User should be able to make payments using multiple vendors like stripe, paypal etc
2. System should be able to handle all the payment related requests like create payment, update payment etc
3. System should handle concurrency issues and make sure we do not cause double payments.
4. System should be able to reconcile pending transactions and send notifications to users.

APIS
GET /v1/payment/modes
    Header: userId, productType
    responseBody:
    {
        UPI: {
                GOOGLE PAY, CRED
            }
        CREDIT_CARD,
        DEBIT_CARD
    }

POST /v1/payment/initiate
   request body:
   headers: userID 
   {
        "amount": 100,
        "transactionType": "UPI/CREDIT_CARD"
        "credBlock": "afddasfasdd" // just for representation nothing to do with it
   } 
  response body:
  {
    "id": "payment123", // unique payment ID
    "status": "pending",
    "amount": 100,
    "description": "Payment for order #123",
  }

GET /v1/payment/status
    Header: userId, paymentID
    {
        status: Pending/success/failure
    }

Database Schema(Postgres):
transactions: 
    - id
    - idempotency_key -- uniquely generated payment ID
    - status
    - userId
    - amount
    - payment_mode
    - vendorTransactionId
    - payment_provider

reconciliation
    - id
    - vendor_transaction_id
    - vendor_status
    - created_at

payment_modes:
    - id
    - UUID
    - payment_mode // UPI, CREDIT_CARd
    - type: CREDIT_debit

API Flows
1.  /v1/payment/modes
 - Check the payment modes available for the user
 - Check the payment modes available for the product type
 - Check the available payment modes for the vendor
 - returns with the json response of the available payment modes


2. /v1/payment/initiate
   - check the payment modes available for the user and product type
   - create a unique payment idempotency key 
     a. Use Redis incr to generate a payment key (If redis check the last paymentID generated and get the incr from it)
     b. Check if the key exists in the database
     c. If it exists, generate a new key
     d. If it does not exist, use the key as the payment id
   - call the payment provider to initiate the payment (Mock this entirely for the vendor and generate a vendor transaction id)
   - store the payment idempotency key in the database in transactions table
   - return the payment idempotency key in the response


3. /v1/payment/status
   - Check the payment status for the payment idempotency key
   - return the payment status in the response


4. /v1/payment/reconcile
   - check the pending transactions (Mock the vendor response and add terminal status of the transactions)
   - return the payment status in the response
