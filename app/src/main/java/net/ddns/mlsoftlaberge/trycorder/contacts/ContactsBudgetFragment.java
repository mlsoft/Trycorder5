/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ddns.mlsoftlaberge.trycorder.contacts;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;


import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.ddns.mlsoftlaberge.trycorder.R;

import java.util.Vector;

/**
 * This fragment displays admins of a specific contact from the contacts provider. It shows the
 * contact's display photo, name and all its mailing addresses. You can also modify this fragment
 * to show other information, such as phone numbers, email addresses and so forth.
 */
public class ContactsBudgetFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_CONTACT_URI =
            "net.ddns.mlsoftlaberge.trycorder.contacts.EXTRA_CONTACT_URI";

    public Uri mContactUri; // Stores the contact Uri for this fragment instance
    public Uri mDataUri;    // stores the data uri
    public int nbloaders;   // stores the nb loaders who are finished

    // structure of a row of the transaction table
    private class Transac {
        String trdate;
        String amount;
        String descrip;
    }

    private class Clients {
        public String id;
        public String name;
        public String rawid;
        public String phone;
        public String phonetypelabel;
        public String address;
        public String addresstypelabel;
        public String email;
        public String emailtypelabel;
        // tables containing the expanded note data
        public int nbtransac;
        public Vector<Transac> transaclist = new Vector<Transac>(10, 10);

    }

    // table containing the list of clients from the database
    private int nbclients=0;
    private Clients clientslist[] = new Clients[500];

    public int getClient(String id) {
        for (int i = 0; i < nbclients; ++i) {
            if (clientslist[i].id.equals(id)) return (i);
        }
        if (nbclients >= 500) return (0);
        clientslist[nbclients] = new Clients();
        clientslist[nbclients].id = id;
        clientslist[nbclients].nbtransac = 0;
        nbclients++;
        return (nbclients - 1);
    }

    // holders for the widgets
    private LinearLayout budget_layout;

    /**
     * Fragments require an empty constructor.
     */
    public ContactsBudgetFragment() {
    }

    /**
     * When the Fragment is first created, this callback is invoked. It initializes some key
     * class fields.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Let this fragment contribute menu items
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflates the main layout to be used by this fragment
        final View budgetView = inflater.inflate(R.layout.contacts_budget_fragment, container, false);

        budget_layout = (LinearLayout) budgetView.findViewById(R.id.budget_layout);

        return budgetView;
    }

    // set the contact URI with the new activity or saved activity state
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Uri uri;
        // If not being created from a previous state
        if (savedInstanceState == null) {
            // Sets the argument extra as the currently displayed contact
            uri = (getArguments() != null ? (Uri) getArguments().getParcelable(EXTRA_CONTACT_URI) : null);
        } else {
            // If being recreated from a saved state, sets the contact from the incoming
            // savedInstanceState Bundle
            uri = ((Uri) savedInstanceState.getParcelable(EXTRA_CONTACT_URI));
        }
        setContact(uri);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * When the Fragment is being saved in order to change activity state, save the
     * currently-selected contact.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saves the contact Uri
        outState.putParcelable(EXTRA_CONTACT_URI, mContactUri);
    }

    // create the menu option to modify this contact
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflates the options menu for this fragment
        inflater.inflate(R.menu.contacts_budget_menu, menu);
    }

    // open the edit-contact activity when asked by the top menu option
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // When "edit" menu option selected
            case R.id.menu_budget_edit:
                // Standard system edit contact intent
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the contact that this Fragment displays, or clears the display if the contact argument
     * is null. This will re-initialize all the views and start the queries to the system contacts
     * provider to populate the contact information.
     *
     * @param contactLookupUri The contact lookup Uri to load and display in this fragment. Passing
     *                         null is valid and the fragment will display a message that no
     *                         contact is currently selected instead.
     */
    public void setContact(Uri contactLookupUri) {

        // A content URI for the Contacts table
        mContactUri = Contacts.CONTENT_URI;
        mDataUri = Uri.parse("content://com.android.contacts/data");

        // If the Uri contains data, load the contact's image and load contact admins.
        if (mContactUri != null) {
            // Starts two queries to to retrieve contact information from the Contacts Provider.
            // restartLoader() is used instead of initLoader() as this method may be called
            // multiple times.
            nbclients = 0;
            nbloaders = 0;
            getLoaderManager().restartLoader(ContactDetailQuery.QUERY_ID, null, this);
            getLoaderManager().restartLoader(ContactAddressQuery.QUERY_ID, null, this);
            getLoaderManager().restartLoader(ContactPhoneQuery.QUERY_ID, null, this);
            getLoaderManager().restartLoader(ContactEmailQuery.QUERY_ID, null, this);
            getLoaderManager().restartLoader(ContactNotesQuery.QUERY_ID, null, this);
        }
    }

    // create a loader to find the results of the contact URI for every querys
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            // Two main queries to load the required information
            case ContactDetailQuery.QUERY_ID:
                // This query loads main contact admins, see
                // ContactDetailQuery for more information.
                return new CursorLoader(getActivity(), mContactUri,
                        ContactDetailQuery.PROJECTION,
                        ContactDetailQuery.SELECTION,
                        null, ContactDetailQuery.SORT_ORDER);
            case ContactAddressQuery.QUERY_ID:
                // This query loads contact address admins, see
                // ContactAddressQuery for more information.
                final Uri uri = Uri.withAppendedPath(mContactUri, Contacts.Data.CONTENT_DIRECTORY);
                return new CursorLoader(getActivity(), mDataUri,
                        ContactAddressQuery.PROJECTION,
                        ContactAddressQuery.SELECTION,
                        null, null);
            case ContactNotesQuery.QUERY_ID:
                // This query loads contact address admins, see
                // ContactAddressQuery for more information.
                final Uri nuri = Uri.withAppendedPath(mContactUri, Contacts.Data.CONTENT_DIRECTORY);
                return new CursorLoader(getActivity(), mDataUri,
                        ContactNotesQuery.PROJECTION,
                        ContactNotesQuery.SELECTION,
                        null, null);
            case ContactPhoneQuery.QUERY_ID:
                // This query loads contact address admins, see
                // ContactAddressQuery for more information.
                final Uri puri = Uri.withAppendedPath(mContactUri, Contacts.Data.CONTENT_DIRECTORY);
                return new CursorLoader(getActivity(), mDataUri,
                        ContactPhoneQuery.PROJECTION,
                        ContactPhoneQuery.SELECTION,
                        null, null);
            case ContactEmailQuery.QUERY_ID:
                // This query loads contact address admins, see
                // ContactAddressQuery for more information.
                final Uri euri = Uri.withAppendedPath(mContactUri, Contacts.Data.CONTENT_DIRECTORY);
                return new CursorLoader(getActivity(), mDataUri,
                        ContactEmailQuery.PROJECTION,
                        ContactEmailQuery.SELECTION,
                        null, null);
        }
        return null;
    }

    // reset the loader
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Nothing to do here. The Cursor does not need to be released as it was never directly
        // bound to anything (like an adapter).
    }


    // process the loader finished request
    // fill all fields on screen, and all layouts with data from the querys
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // If this fragment was cleared while the query was running
        // eg. from from a call like setContact(uri) then don't do
        // anything.
        if (mContactUri == null) {
            return;
        }
        if (data == null) {
            return;
        }
        // collect datas depending on the loader id
        switch (loader.getId()) {
            case ContactDetailQuery.QUERY_ID:
                // Moves to the first row in the Cursor
                if (data.moveToFirst()) {
                    do {
                        String contactid = data.getString(ContactDetailQuery.ID);
                        String rawcontactid = data.getString(ContactDetailQuery.RAWID);
                        String contactName = data.getString(ContactDetailQuery.DISPLAY_NAME);
                        // place the datas in the clients table
                        int clientno = getClient(rawcontactid);
                        clientslist[clientno].name = contactName;
                        clientslist[clientno].rawid = rawcontactid;
                    } while (data.moveToNext());
                }
                nbloaders++;
                break;
            case ContactAddressQuery.QUERY_ID:
                // This query loads the contact address .
                if (data.moveToFirst()) {
                    do {
                        // Builds the address layout
                        String contactid = data.getString(ContactAddressQuery.ID);
                        String rawcontactid = data.getString(ContactAddressQuery.RAWID);
                        int addrtype = data.getInt(ContactAddressQuery.TYPE);
                        String addrlabel = data.getString(ContactAddressQuery.LABEL);
                        String addrdata = data.getString(ContactAddressQuery.ADDRESS);
                        // Gets postal address label type
                        CharSequence alabel =
                                StructuredPostal.getTypeLabel(getResources(), addrtype, addrlabel);
                        // place the datas in the clients table
                        int clientno = getClient(rawcontactid);
                        clientslist[clientno].address = addrdata;
                        clientslist[clientno].addresstypelabel = alabel.toString();
                    } while (data.moveToNext());
                }
                nbloaders++;
                break;
            case ContactPhoneQuery.QUERY_ID:
                // This query loads the contact phone
                if (data.moveToFirst()) {
                    do {
                        String contactid = data.getString(ContactPhoneQuery.ID);
                        String rawcontactid = data.getString(ContactPhoneQuery.RAWID);
                        int phonetype = data.getInt(ContactPhoneQuery.TYPE);
                        String phonelabel = data.getString(ContactPhoneQuery.LABEL);
                        String phonedata = data.getString(ContactPhoneQuery.PHONE);
                        // Gets phone label type
                        CharSequence plabel =
                            android.provider.ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                    getResources(), phonetype, phonelabel);
                        // place the datas in the clients table
                        int clientno = getClient(rawcontactid);
                        clientslist[clientno].phone = phonedata;
                        clientslist[clientno].phonetypelabel = plabel.toString();
                    } while (data.moveToNext());
                }
                nbloaders++;
                break;
            case ContactEmailQuery.QUERY_ID:
                // This query loads the contact email
                if (data.moveToFirst()) {
                    do {
                        String contactid = data.getString(ContactEmailQuery.ID);
                        String rawcontactid = data.getString(ContactEmailQuery.RAWID);
                        int emailtype = data.getInt(ContactEmailQuery.TYPE);
                        String emaillabel = data.getString(ContactEmailQuery.LABEL);
                        String emaildata = data.getString(ContactEmailQuery.EMAIL);
                        // Gets email label type
                        CharSequence elabel =
                                android.provider.ContactsContract.CommonDataKinds.Email.getTypeLabel(
                                        getResources(), emailtype, emaillabel);
                        // place the datas in the clients table
                        int clientno = getClient(rawcontactid);
                        clientslist[clientno].email = emaildata;
                        clientslist[clientno].emailtypelabel = elabel.toString();
                    } while (data.moveToNext());
                }
                nbloaders++;
                break;
            case ContactNotesQuery.QUERY_ID:
                // This query loads the contact notes
                // Get the first row of the cursor (table contains only one row)
                if (data.moveToFirst()) {
                    do {
                        String contactid = data.getString(ContactNotesQuery.ID);
                        String rawcontactid = data.getString(ContactNotesQuery.RAWID);
                        String notesdata = data.getString(ContactNotesQuery.NOTE);
                        // place the datas in the clients table
                        int clientno = getClient(rawcontactid);
                        expandnote(clientno,notesdata);
                    } while (data.moveToNext());
                }
                nbloaders++;
                break;
        }
        // when all loaders are finished, then display the contents
        if(nbloaders>=5) {
            filltransactionlayout();
        }

    }

    // --------------------------------------------------------------------
    // this is the decoding/encoding part of the transaction table
    // --------------------------------------------------------------------

    // expand the note in a table of fields
    // scan mNotesData and cut it in fields and tables
    private void expandnote(int clientno, String notesdata) {
        // start with an empty list of transactions in the client
        clientslist[clientno].nbtransac=0;
        clientslist[clientno].transaclist.removeAllElements();
        // expand note in lines
        int i = 0;
        int p;
        while (i < notesdata.length()) {
            // find the position of the end of line (may be end of block too)
            p = notesdata.indexOf('\n', i);
            if (p < 0) {
                p = notesdata.length() - 1;
            }
            String noteline = notesdata.substring(i, p + 1);
            // if an admin line, then decode it
            // else add it to the memo field
            if (noteline.indexOf("ADMIN|") == 0) {
                decodeline(clientno,noteline);
            }
            // advance after the last char eated by line
            i = p + 1;
        }
    }

    // decode the noteline beginning by ADMIN| and add it to the table
    private void decodeline(int clientno, String noteline) {
        // create an empty default transaction
        Transac transac = new Transac();
        // decode the line
        int i = 6;
        int p;
        // search for the date
        p = noteline.indexOf('|', i);
        if (p < 0) p = noteline.length() - 1;
        if (p >= i) {
            transac.trdate = noteline.substring(i, p);
            i = p + 1;
            // search for the amount
            p = noteline.indexOf('|', i);
            if (p < 0) p = noteline.length() - 1;
            if (p >= i) {
                transac.amount = noteline.substring(i, p);
                i = p + 1;
                // search for the description
                p = noteline.indexOf('|', i);
                if (p < 0) p = noteline.indexOf('\n', i);
                if (p < 0) p = noteline.length();
                if (p >= i) {
                    transac.descrip = noteline.substring(i, p);
                    i = p + 1;
                }
            }
        }
        // add element to the table
        clientslist[clientno].transaclist.addElement(transac);
        clientslist[clientno].nbtransac++;
    }

    /**
     * This interface defines constants used by contact retrieval queries.
     */
    public interface ContactDetailQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 1;

        // The query projection (columns to fetch from the provider)
        @SuppressLint("InlinedApi")
        final static String[] PROJECTION = {
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.STARRED,
                Contacts.LOOKUP_KEY,
                Contacts.PHOTO_URI,
                Contacts.NAME_RAW_CONTACT_ID,
        };

        @SuppressLint("InlinedApi")
        final static String SELECTION =
                Contacts.DISPLAY_NAME_PRIMARY + "<>''"
                        + " AND " + Contacts.IN_VISIBLE_GROUP + "=1";

        @SuppressLint("InlinedApi")
        final static String SORT_ORDER = Contacts.SORT_KEY_PRIMARY;


        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int DISPLAY_NAME = 1;
        final static int STARRED = 2;
        final static int LOOKUP_KEY = 3;
        final static int PHOTO_URI = 4;
        final static int RAWID = 5;
    }

    /**
     * This interface defines constants used by address retrieval queries.
     */
    public interface ContactAddressQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 2;

        // The query projection (columns to fetch from the provider)
        final static String[] PROJECTION = {
                StructuredPostal._ID,
                StructuredPostal.FORMATTED_ADDRESS,
                StructuredPostal.TYPE,
                StructuredPostal.LABEL,
                StructuredPostal.RAW_CONTACT_ID,
        };

        // The query selection criteria. In this case matching against the
        // StructuredPostal content mime type.
        final static String SELECTION =
                Data.MIMETYPE + "='" + StructuredPostal.CONTENT_ITEM_TYPE + "'";

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int ADDRESS = 1;
        final static int TYPE = 2;
        final static int LABEL = 3;
        final static int RAWID = 4;
    }

    /**
     * This interface defines constants used by address retrieval queries.
     */
    public interface ContactNotesQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 3;

        // The query projection (columns to fetch from the provider)
        final static String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Note._ID,
                ContactsContract.CommonDataKinds.Note.NOTE,
                ContactsContract.CommonDataKinds.Note.RAW_CONTACT_ID,
        };

        // The query selection criteria. In this case matching against the
        // Note content mime type.
        final static String SELECTION =
                Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE + "'";

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int NOTE = 1;
        final static int RAWID = 2;
    }

    /**
     * This interface defines constants used by address retrieval queries.
     */
    public interface ContactPhoneQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 4;

        // The query projection (columns to fetch from the provider)
        final static String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
        };

        // The query selection criteria. In this case matching against the
        // Note content mime type.
        final static String SELECTION =
                Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int PHONE = 1;
        final static int TYPE = 2;
        final static int LABEL = 3;
        final static int RAWID = 4;
    }

    /**
     * This interface defines constants used by address retrieval queries.
     */
    public interface ContactEmailQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 5;

        // The query projection (columns to fetch from the provider)
        final static String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email._ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.LABEL,
                ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID,
        };

        // The query selection criteria. In this case matching against the
        // Note content mime type.
        final static String SELECTION =
                Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int EMAIL = 1;
        final static int TYPE = 2;
        final static int LABEL = 3;
        final static int RAWID = 4;
    }

    private LinearLayout.LayoutParams tlayoutParams;
    private float gtot;

    // fill the transactions layout with the transaction views
    private void filltransactionlayout() {
        // Each LinearLayout has the same LayoutParams so this can
        // be created once and used for each cumulative layouts of data
        tlayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        // Clears out the details layout first in case the details
        // layout has data from a previous data load still
        // added as children.
        budget_layout.removeAllViews();
        gtot = 0.0f;
        // loop thru all the clients
        for (int i = 0; i < nbclients; ++i) {
            if (clientslist[i].nbtransac != 0) {
                fillclientlayout(i);
            }
        }
        // add a row with the gtot amount
        LinearLayout tlayout = (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                R.layout.contacts_budget_trans, null, false);
        TextView descrip = (TextView) tlayout.findViewById(R.id.budget_trans_descrip);
        TextView amount = (TextView) tlayout.findViewById(R.id.budget_trans_amount);
        descrip.setText("TOTAL");
        descrip.setTextSize(18.0f);
        amount.setText(String.format("%.2f",gtot));
        amount.setTextSize(18.0f);
        budget_layout.addView(tlayout,tlayoutParams);
    }

    public void fillclientlayout(int clientno) {
        LinearLayout clayout = (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                R.layout.contacts_budget_item, null, false);
        TextView id = (TextView) clayout.findViewById(R.id.budget_id);
        TextView name = (TextView) clayout.findViewById(R.id.budget_name);
        TextView phone = (TextView) clayout.findViewById(R.id.budget_phone);
        TextView email = (TextView) clayout.findViewById(R.id.budget_email);
        TextView address = (TextView) clayout.findViewById(R.id.budget_address);
        LinearLayout blayout = (LinearLayout) clayout.findViewById(R.id.budget_trans);
        id.setText(clientslist[clientno].id);
        name.setText(clientslist[clientno].name);
        phone.setText(clientslist[clientno].phone);
        email.setText(clientslist[clientno].email);
        address.setText(clientslist[clientno].address);
        blayout.removeAllViews();
        // loop thru all transactions of the client
        for (int i = 0; i < clientslist[clientno].nbtransac; ++i) {
            // Builds the transaction layout
            // Inflates the transaction layout
            LinearLayout tlayout = (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                    R.layout.contacts_budget_trans, null, false);
            TextView descrip = (TextView) tlayout.findViewById(R.id.budget_trans_descrip);
            TextView amount = (TextView) tlayout.findViewById(R.id.budget_trans_amount);
            // get the current transaction
            Transac transac = clientslist[clientno].transaclist.elementAt(i);
            // fill the fields with the table data
            descrip.setText(transac.descrip);
            amount.setText(transac.amount);
            // cumulate the total amount
            gtot += Double.valueOf(transac.amount);
            // Adds the new note layout to the notes layout
            blayout.addView(tlayout, tlayoutParams);
        }
        // add the client layout in the budget layout
        budget_layout.addView(clayout,tlayoutParams);
    }


}
