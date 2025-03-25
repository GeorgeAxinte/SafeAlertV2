package com.example.safealertv2;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import java.util.ArrayList;
import java.util.List;

public class FavoriteContactsHelper {

    private final Context context;

    public FavoriteContactsHelper(Context context) {
        this.context = context;
    }

    public List<String> getFavoriteContacts() {
        List<String> favoriteContacts = new ArrayList<>();
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String selection = ContactsContract.Contacts.STARRED + "=?";
        String[] selectionArgs = new String[]{"1"};
        Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, null);

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);

            while (cursor.moveToNext()) {
                String contactId = cursor.getString(idIndex);
                String contactName = cursor.getString(nameIndex);
                String phoneNumber = getPhoneNumber(contactId);

                if (phoneNumber != null) {
                    favoriteContacts.add(phoneNumber);
                }
            }
            cursor.close();
        }
        return favoriteContacts;
    }

    private String getPhoneNumber(String contactId) {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?";
        String[] selectionArgs = new String[]{contactId};
        Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String phoneNumber = cursor.getString(numberIndex);
                cursor.close();
                return phoneNumber;
            }
            cursor.close();
        }
        return null;
    }
}
