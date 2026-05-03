# World Bank: Mobile Banking App

World Bank is an Android banking application built for the Pakistani market. It lets users send money, manage cards, pay bills, and track their spending from a single app.

---

## Demo Video: https://youtu.be/1mrV-b5_1yU
## Features

**Authentication**

- Email and password sign up with full field validation
- Duplicate CNIC and phone number detection at registration
- Email verification required before first login
- Forgot password via email reset link
- Remember me saves the email field for the next login
- Session management with auto login on app relaunch

**Home Screen**

- Displays account balance and linked card
- Quick action buttons for Transfer, Payments, Top Up, and Add Card
- Recent transaction history with live Firestore data
- Bottom navigation bar for main sections

**Send Money**

- Internal transfers between World Bank accounts (free)
- IBFT transfers to external Pakistani banks with Rs. 25 fee
- JazzCash wallet transfers
- Saved contacts for quick recipient selection
- IBAN validation with locked PK36WBNK prefix
- Transfer success screen with receipt and reference number

**Top Up**

- Add balance to account from external source
- Confirm screen before processing

**Payments**

- Bill payments to registered billers
- Add payee flow with consumer ID entry
- Payment settings management

**Cards**

- View linked debit card with masked number
- Card detail and statistics screens
- Add new card flow
- Monthly spending limit tracking

**Transaction History**

- Full list of past transactions
- Debit and credit entries with timestamps
- Reference numbers for each transaction

**Budgets**

- Set a total monthly spending goal
- Visual breakdown of spending by category
- Live progress against set limits

**Account and Profile**

- View personal information including name, email, phone
- Edit city and location
- Locked fields for sensitive info like CNIC and phone with clear indicators
- Privacy and security screen with change password option

**Security**

- Firebase Authentication for all login flows
- Firestore security rules restrict data access to authenticated users only
- Email verification enforced before account access
- CNIC and phone uniqueness enforced at registration
- Passwords never stored locally

---

## Tech Stack

- Language: Java
- Platform: Android (minimum SDK 24)
- Backend: Firebase Authentication, Firestore
- Architecture: Activity based with Repository pattern for Firestore operations
- UI: Material Components, ConstraintLayout, CardView

---

## Project Structure

```
app/
  src/main/
    java/com/worldbank/app/
      activities/       All screens and UI logic
      models/           Data classes for User, Account, Card, Transaction, Contact
      utils/            SessionManager, TransactionRepository
    res/
      layout/           XML layouts for all screens and dialogs
      drawable/         Backgrounds, shapes, icons
      values/           Colors, strings, dimensions, themes
```

---

## How to Run

**Requirements**

- Android Studio Hedgehog or later
- Java 11 or higher
- An Android device or emulator running API 24 or above

**Steps**

1. Clone or download this repository
2. Open the project in Android Studio
3. Wait for Gradle sync to complete
4. Place your own google-services.json file inside the app/ folder
5. Enable Email/Password authentication in your Firebase Console under Authentication
6. Set up Cloud Firestore in your Firebase project
7. Apply the following Firestore security rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    match /accounts/{accountId} {
      allow read, write: if request.auth != null;
    }
    match /cards/{cardId} {
      allow read, write: if request.auth != null;
    }
    match /transactions/{txnId} {
      allow read, write: if request.auth != null;
    }
    match /contacts/{contactId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

8. Connect a device or start an emulator
9. Click Run in Android Studio

---

## Notes

- All monetary values are in Pakistani Rupees (PKR)
- IBAN format follows the Pakistani standard: PK36WBNK followed by 16 digits
- Internal transfers between World Bank accounts are instant and free
- IBFT transfers to external banks carry a Rs. 25 admin fee
- The app is locked to portrait orientation

---

## Team

Built by Fatima and Aneeza as a university project covering real world banking flows with Firebase as the backend.
