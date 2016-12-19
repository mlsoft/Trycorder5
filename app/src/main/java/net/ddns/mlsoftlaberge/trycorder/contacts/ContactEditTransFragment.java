package net.ddns.mlsoftlaberge.trycorder.contacts;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.Fragment;

import net.ddns.mlsoftlaberge.trycorder.R;

/**
 * Created by mlsoft on 28/02/16.
 */
public class ContactEditTransFragment extends Fragment {
    public static final String EXTRA_CONTACT_URI =
            "net.ddns.mlsoftlaberge.trycorder.contacts.EXTRA_CONTACT_URI";

    // Defines a tag for identifying log entries
    private static final String TAG = "ContactEditTransFragment";

    private Uri mContactUri; // Stores the contact Uri for this fragment instance

    public String contactName = "";   // stores the contact name for this instance

    public String transDescrip = "";   // stores the transaction description for this instance

    public String transAmount = "";   // stores the amount data for this instance

    public String transDate = "";     // stores the date of transaction

    private TextView mContactName;  // display contact name

    private EditText mEditDescrip;     // display and edit the memo field
    private EditText mEditAmount;     // display and edit the memo field
    private EditText mEditDate;     // display and edit the memo field

    private Button mEditTransButton; // save the data and return to previous avtivity

    private Button mDeleteTransButton; // save the data and return to previous avtivity

    private Button mBackButton;
    private ImageButton mBacktopButton;
    /**
     * Factory method to generate a new instance of the fragment given a contact Uri. A factory
     * method is preferable to simply using the constructor as it handles creating the bundle and
     * setting the bundle as an argument.
     *
     * @param contactUri The contact Uri to load
     * @return A new instance of {@link ContactEditMemoFragment}
     */
    public static ContactEditTransFragment newInstance(Uri contactUri, String name, String descrip, String amount, String date) {
        // Create new instance of this fragment
        final ContactEditTransFragment fragment = new ContactEditTransFragment();

        fragment.contactName = name;
        fragment.transDescrip = descrip;
        fragment.transAmount = amount;
        fragment.transDate = date;

        // Create and populate the args bundle
        final Bundle args = new Bundle();
        args.putParcelable(EXTRA_CONTACT_URI, contactUri);

        // Assign the args bundle to the new fragment
        fragment.setArguments(args);

        // Return fragment
        return fragment;
    }

    /**
     * Fragments require an empty constructor.
     */
    public ContactEditTransFragment() {
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

        // In version 3.0 and later, stores the provided contact lookup Uri in a class field. This
        // Uri is then used at various points in this class to map to the provided contact.
        if (Utils.hasHoneycomb()) {
            mContactUri = contactLookupUri;
        } else {
            // For versions earlier than Android 3.0, stores a contact Uri that's constructed from
            // contactLookupUri. Later on, the resulting Uri is combined with
            // Contacts.Data.CONTENT_DIRECTORY to map to the provided contact. It's done
            // differently for these earlier versions because Contacts.Data.CONTENT_DIRECTORY works
            // differently for Android versions before 3.0.
            mContactUri = ContactsContract.Contacts.lookupContact(getActivity().getContentResolver(),
                    contactLookupUri);
        }
        if (mContactName != null) {
            mContactName.setText(contactName);
        }
        if (mEditDescrip != null) {
            mEditDescrip.setText(transDescrip);
        }
        if (mEditAmount != null) {
            mEditAmount.setText(transAmount);
        }
        if (mEditDate != null) {
            mEditDate.setText(transDate);
        }
    }

    /**
     * When the Fragment is first created, this callback is invoked. It initializes some key
     * class fields.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflates the main layout to be used by this fragment
        final View adminView =
                inflater.inflate(R.layout.contact_edittrans_fragment, container, false);

        mContactName = (TextView) adminView.findViewById(R.id.contact_name);
        mContactName.setVisibility(View.VISIBLE);

        mEditDescrip = (EditText) adminView.findViewById(R.id.trans_description);
        mEditDescrip.setVisibility(View.VISIBLE);

        mEditAmount = (EditText) adminView.findViewById(R.id.trans_amount);
        mEditAmount.setVisibility(View.VISIBLE);

        mEditDate = (EditText) adminView.findViewById(R.id.trans_date);
        mEditDate.setVisibility(View.VISIBLE);

        // Defines an onClickListener object for the add-admin button
        mEditTransButton = (Button) adminView.findViewById(R.id.button_save_edittrans);
        mEditTransButton.setOnClickListener(new View.OnClickListener() {
            // Defines what to do when users click the address button
            @Override
            public void onClick(View view) {
                buttonsound();
                // Displays a message that no activity can handle the view button.
                Toast.makeText(getActivity(), "Save EditTrans", Toast.LENGTH_SHORT).show();
                returnresult(true);
            }
        });

        // Defines an onClickListener object for the add-admin button
        mDeleteTransButton = (Button) adminView.findViewById(R.id.button_delete_edittrans);
        mDeleteTransButton.setOnClickListener(new View.OnClickListener() {
            // Defines what to do when users click the address button
            @Override
            public void onClick(View view) {
                buttonsound();
                // Displays a message that no activity can handle the view button.
                Toast.makeText(getActivity(), "Delete EditTrans", Toast.LENGTH_SHORT).show();
                // create a popup menu to confirm deletion
                PopupMenu popup = new PopupMenu(getActivity(), view);
                popup.getMenuInflater().inflate(R.menu.popupdel, popup.getMenu());
                // catch the selection of the popup
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        String title = String.valueOf(item.getTitle());
                        if (title.equals("DELETE")) {
                            returnresult(false);
                        } else {
                            Toast.makeText(getActivity(), "Clicked popup menu item " + title,
                                    Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });
                // start the popup
                popup.show();
            }
        });

        // Defines an onClickListener object for the add-admin button
        mBackButton = (Button) adminView.findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            // Defines what to do when users click the address button
            @Override
            public void onClick(View view) {
                buttonsound();
                getActivity().finish();
            }
        });

        // Defines an onClickListener object for the add-admin button
        mBacktopButton = (ImageButton) adminView.findViewById(R.id.backtop_button);
        mBacktopButton.setOnClickListener(new View.OnClickListener() {
            // Defines what to do when users click the address button
            @Override
            public void onClick(View view) {
                buttonsound();
                getActivity().finish();
            }
        });

        return adminView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // If not being created from a previous state
        if (savedInstanceState == null) {
            // Sets the argument extra as the currently displayed contact
            setContact(getArguments() != null ?
                    (Uri) getArguments().getParcelable(EXTRA_CONTACT_URI) : null);
        } else {
            // If being recreated from a saved state, sets the contact from the incoming
            // savedInstanceState Bundle
            setContact((Uri) savedInstanceState.getParcelable(EXTRA_CONTACT_URI));
        }
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

    // returns the contents of edited data to the caller activity
    public void returnresult(boolean saveordel) {
        final Intent data = new Intent();
        if (saveordel) {
            data.putExtra("DESCRIP", mEditDescrip.getText().toString());
            data.putExtra("AMOUNT", mEditAmount.getText().toString());
            data.putExtra("DATE", mEditDate.getText().toString());
        } else {
            data.putExtra("DESCRIP", "");
            data.putExtra("AMOUNT", "");
            data.putExtra("DATE", "");
        }
        getActivity().setResult(Activity.RESULT_OK, data);
        getActivity().finish();
    }

    private void buttonsound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(getActivity().getBaseContext(), R.raw.keyok2);
        mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }

}
