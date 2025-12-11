# Delete Account Feature - AllowanceAlley Analysis and MyBible Implementation Plan

## Part 1: AllowanceAlley Delete Account Behavior (Reference)

### 1.1 Access Path
- User clicks **Settings** from the Dashboard (shown as a button/action card with gear icon)
- Settings page is located at `/settings` endpoint

### 1.2 Settings Page UI Structure

**Header Section:**
- Purple gradient background (`linear-gradient(135deg, #667eea 0%, #764ba2 100%)`)
- Title: "Settings" with gear emoji
- Right side shows: Family Name (bold) and Email (smaller, 80% opacity)
- "Dashboard" button (link styled as button) to return

**Page Layout:**
- Max width: 800px, centered
- White card sections with rounded corners and shadow

**Danger Zone Section:**
- Red border (`2px solid #dc3545`)
- Light red/pink background (`#fff5f5`)
- Title: "Danger Zone" in red with warning emoji
- Warning box (yellow/amber background `#fff3cd`):
  > "**Warning:** Deleting your family account is permanent and cannot be undone. All family members, chores, assignments, rewards, redemptions, and activity history will be permanently deleted."
- Instruction text: "To delete your family account, click the button below and confirm by typing your family name."
- **Red "Delete Family Account" button** (`btn-danger`)

### 1.3 Delete Confirmation UI (Hidden by default)

When user clicks "Delete Family Account":
1. A confirmation div slides open (CSS class `confirm-delete.show`)
2. Background: `#f8d7da` (light red)
3. Contains a FORM with:
   - Hidden input: `name='action' value='deleteFamily'`
   - Instruction: "**To confirm, type your family name:** `<code>FamilyName</code>`"
   - Text input field:
     - ID: `confirmFamilyName`
     - Name: `confirmFamilyName`
     - Placeholder: "Type family name to confirm"
     - Red border (`2px solid #dc3545`)
   - Two buttons:
     - **"Permanently Delete Family"** (red, `btn-danger`)
     - **"Cancel"** (gray, `btn-system`)
4. Form posts to `/settings` with `onsubmit='return validateDelete()'`

### 1.4 Client-Side Validation (JavaScript)

```javascript
function validateDelete() {
    var input = document.getElementById('confirmFamilyName').value.trim();
    var expected = 'FamilyName'; // JS-escaped family name
    console.log('Comparing input:', input, 'with expected:', expected);
    if (input !== expected) {
        alert('Family name does not match. Please type: ' + expected);
        return false;
    }
    return confirm('Are you absolutely sure? This action CANNOT be undone!');
}
```

**Key Points:**
- Exact string match (case-sensitive)
- Console logging for debugging
- Alert if mismatch
- Final browser confirmation dialog

### 1.5 Server-Side Processing (POST /settings)

**Validation:**
```java
String confirmName = request.getParameter("confirmFamilyName");
if (confirmName == null || !confirmName.trim().equals(familyName)) {
    String errorMsg = "Family name confirmation did not match...";
    response.sendRedirect("/settings?error=" + URLEncoder.encode(errorMsg, "UTF-8"));
    return;
}
```

**AllowanceAlley Deletion Order (11 tables with foreign key constraints):**
1. activity_ledger (references family_members)
2. points_ledger (references family_members)
3. redemptions (references family_members and rewards)
4. chore_completions (references chore_assignments)
5. chore_assignments (references chores and family_members)
6. rewards (references family)
7. chores (references family)
8. family_members (references family)
9. app_settings (for this family)
10. auth_users (references family)
11. families

**Delete Method Used:**
```java
// Uses setSelectClause with raw SQL DELETE statement
String deleteAuthUsers = "DELETE FROM auth_users WHERE FAMILY_ID = '" + familyId + "'";
com.esarks.arm.model.jeo.ServiceJeo delJeo = new com.esarks.arm.model.jeo.ServiceJeo();
delJeo.getRequest().setSelectClause(deleteAuthUsers);
delJeo.getRequest().setWhereClause("1=1");
authUsersCrud.deleteAUTH_USERS(delJeo);
```

### 1.6 Post-Deletion Actions

1. Invalidate HTTP session: `session.invalidate()`
2. Clear auth_token cookie:
   ```java
   Cookie authCookie = new Cookie("auth_token", "");
   authCookie.setPath("/");
   authCookie.setMaxAge(0);
   authCookie.setHttpOnly(true);
   response.addCookie(authCookie);
   ```
3. Redirect to: `/login?deleted=true`

### 1.7 Confirmation Page (Alternative Flow)

There's also a `/delete-account` endpoint that shows a standalone confirmation page after deletion:
- Title: "Account Deleted" with green checkmark
- Message: "Your account and all associated data have been permanently deleted. Thank you for using AllowanceAlley."
- "Return to Home" button

---

## Part 2: MyBible Implementation Plan

### 2.1 MyBible Database Schema (Current)

MyBible currently has **1 table**:

**AUTH_USERS Table:**
| Field | Type | Size | Nullable | Description |
|-------|------|------|----------|-------------|
| ID | string | 36 | NO | Primary key (UUID) |
| EMAIL | string | 255 | NO | User email address |
| PASSWORD_HASH | string | 255 | NO | Hashed password |
| NAME | string | 255 | YES | User display name |
| EMAIL_VERIFIED | boolean | - | NO | Email verification status |
| VERIFICATION_CODE | string | 6 | YES | 6-digit verification code |
| VERIFICATION_CODE_EXPIRES_AT | timestamp | - | YES | Code expiration time |
| CREATED_AT | timestamp | - | NO | Account creation time |
| UPDATED_AT | timestamp | - | NO | Last update time |
| LAST_LOGIN | timestamp | - | YES | Last login time |

### 2.2 Key Differences from AllowanceAlley

| Aspect | AllowanceAlley | MyBible |
|--------|---------------|---------|
| Account Type | Family (multiple users per family) | Individual user (single user) |
| Identifier to Type | Family Name | User's Name |
| Total Tables | 11 tables | 1 table (AUTH_USERS) |
| Delete Complexity | Cascade delete across 11 tables | Single table delete |
| Foreign Keys | Complex relationships | None (single table) |

### 2.3 MyBible Deletion Order (Current)

Since MyBible only has AUTH_USERS, deletion is simple:

1. **AUTH_USERS** - Delete user record by ID

**Future tables (when implemented) will require cascade delete in this order:**
1. (future) reading_progress
2. (future) highlights
3. (future) bookmarks
4. (future) notes
5. AUTH_USERS

### 2.4 Settings Page Requirements

**Header:**
- Dark gradient background (MyBible brand: `#2c3e50` to `#34495e`)
- Title: "Settings"
- Right side: User Name (bold) + Email (smaller)
- "Dashboard" button

**Account Information Section:**
- White card with shadow
- Display: Name, Email, Email Verified status

**Danger Zone Section:**
- Red border, light red background
- Warning box: "**Warning:** Deleting your account is permanent and cannot be undone. All your notes, bookmarks, highlights, and reading progress will be permanently deleted."
- Instruction: "To delete your account, click the button below and confirm by typing your account name."
- "Delete My Account" button (red)

### 2.5 Delete Confirmation Form

**Form Elements:**
- Hidden input: `action=deleteAccount`
- Instruction showing the user's name in `<code>` tags
- Text input field:
  - ID: `confirmName`
  - Name: `confirmName`
  - Placeholder: "Type account name to confirm"
- "Permanently Delete Account" button (red)
- "Cancel" button (gray)

### 2.6 JavaScript Validation

```javascript
function validateDelete() {
    var input = document.getElementById('confirmName').value.trim();
    var expected = 'UserName'; // JS-escaped user name from server
    console.log('Comparing input:', input, 'with expected:', expected);
    if (input !== expected) {
        alert('Account name does not match. Please type: ' + expected);
        return false;
    }
    return confirm('Are you absolutely sure? This action CANNOT be undone!');
}
```

### 2.7 Server-Side POST Handler

**Validation:**
```java
String confirmName = request.getParameter("confirmName");
// Get actual name from DB
String actualName = user.getNAME();
if (actualName == null || actualName.isEmpty()) {
    actualName = "My Account"; // Fallback
}
if (confirmName == null || !confirmName.trim().equals(actualName)) {
    response.sendRedirect("/settings?error=name_mismatch");
    return;
}
```

**Deletion (single table - AUTH_USERS only):**
```java
// Delete from AUTH_USERS table
String deleteSql = "DELETE FROM auth_users WHERE ID = '" + userId.replace("'", "''") + "'";
com.esarks.arm.model.jeo.ServiceJeo deleteJeo = new com.esarks.arm.model.jeo.ServiceJeo();
deleteJeo.getRequest().setSelectClause(deleteSql);
deleteJeo.getRequest().setWhereClause("1=1");
authUsersCrud.deleteAUTH_USERS(deleteJeo);
```

### 2.8 Post-Deletion Actions

1. Clear auth_token cookie:
   ```java
   Cookie authCookie = new Cookie("auth_token", "");
   authCookie.setPath("/");
   authCookie.setMaxAge(0);
   authCookie.setHttpOnly(true);
   response.addCookie(authCookie);
   ```
2. Redirect to `/?deleted=true` (home page with success message)

### 2.9 Home Page Enhancement

Check for `?deleted=true` query parameter:
```java
String deleted = request.getParameter("deleted");
if ("true".equals(deleted)) {
    out.println("<div class='success-message'>Your account has been successfully deleted. Thank you for using MyBible.</div>");
}
```

---

## Part 3: Implementation Checklist

### 3.1 Completed Items

- [x] Add `escapeHtml()` method to Dashboard servlet
- [x] Add `escapeHtml()` method to Settings servlet
- [x] Settings GET: Fetch user name from DB (not just JWT)
- [x] Settings GET: Display user name in header and confirmation prompt
- [x] Settings GET: JavaScript with exact name comparison
- [x] Settings POST: Validate name matches exactly
- [x] Settings POST: Delete using `setSelectClause()` with raw SQL
- [x] Settings POST: Clear cookie and redirect
- [x] Home page: Show success message when `?deleted=true`
- [x] Dashboard: Server-side rendering with user name/email in header
- [x] Dashboard: Settings and Sign Out buttons in header

### 3.2 Pending Items

- [ ] Build with allPhases.bat
- [ ] Deploy to Cloud Run
- [ ] Test delete flow end-to-end

---

## Part 4: CSS Classes Used

```css
.danger-zone { border: 2px solid #dc3545; background: #fff5f5; }
.danger-zone h2 { color: #dc3545; }
.warning-box { background: #fff3cd; border: 1px solid #ffc107; padding: 15px; border-radius: 8px; margin-bottom: 20px; color: #856404; }
.confirm-delete { display: none; margin-top: 20px; padding: 20px; background: #f8d7da; border-radius: 8px; }
.confirm-delete.show { display: block; }
.confirm-delete input[type='text'] { width: 100%; max-width: 300px; padding: 12px 16px; border: 2px solid #dc3545; border-radius: 8px; font-size: 16px; margin: 10px 0; }
.btn-danger { background: #dc3545; color: white; }
.btn-danger:hover { background: #c82333; }
.btn-secondary { background: #6c757d; color: white; }
.error-message { background: #f8d7da; border: 1px solid #dc3545; color: #721c24; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
.success-message { background: #d4edda; border: 1px solid #28a745; color: #155724; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
code { background: #e9ecef; padding: 4px 8px; border-radius: 4px; font-family: monospace; }
```

---

## Part 5: Delete Method Pattern

**IMPORTANT:** AllowanceAlley uses `setSelectClause()` with raw SQL, NOT the JEO-based delete. MyBible follows the same pattern.

```java
// MyBible delete pattern (single table):
String deleteSql = "DELETE FROM auth_users WHERE ID = '" + userId.replace("'", "''") + "'";
com.esarks.arm.model.jeo.ServiceJeo deleteJeo = new com.esarks.arm.model.jeo.ServiceJeo();
deleteJeo.getRequest().setSelectClause(deleteSql);
deleteJeo.getRequest().setWhereClause("1=1");
authUsersCrud.deleteAUTH_USERS(deleteJeo);
```

This approach:
1. Uses the CRUD service (not raw JDBC)
2. Passes the full DELETE SQL via `setSelectClause()`
3. Sets a dummy `WhereClause("1=1")` as required by the API
4. Executes through the standard CRUD delete method
