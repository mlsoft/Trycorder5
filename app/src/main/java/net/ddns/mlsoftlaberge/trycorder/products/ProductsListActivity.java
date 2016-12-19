package net.ddns.mlsoftlaberge.trycorder.products;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import net.ddns.mlsoftlaberge.trycorder.R;

/*
 * ProductsListActivity displays the existing product items
 * in a list
 * 
 * You can create new ones via the ActionBar entry "Insert"
 * You can delete existing ones via a long press on the item
 */

public class ProductsListActivity extends ListActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {
	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;
	private static final int DELETE_ID = Menu.FIRST + 1;
	// private Cursor cursor;
	private SimpleCursorAdapter adapter;

    // buttons of control

    private ImageButton mBacktopButton;
    private Button mBackButton;
    private Button mFillButton;
    private Button mAddButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.product_list);
		this.getListView().setDividerHeight(2);
		fillData();
		registerForContextMenu(getListView());

        // the search button
        mBacktopButton = (ImageButton) findViewById(R.id.backtop_button);
        mBacktopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                buttonback();
            }
        });
        // the search button
        mBackButton = (Button) findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                buttonback();
            }
        });
        // the search button
        mFillButton = (Button) findViewById(R.id.fill_button);
        mFillButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                filltable();
            }
        });
        // the search button
        mAddButton = (Button) findViewById(R.id.button_add_product);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonsound();
                buttonadd();
            }
        });

    }

    private void buttonsound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.keyok2);
        mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }

    private void buttonback() {
        finish();
    }


    // Reaction to the menu selection
	public void buttonadd() {
		createProduct();
	}

    // permits this activity to hide status and action bars, and proceed full screen
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			Uri uri = Uri.parse(MyProductContentProvider.CONTENT_URI + "/"
					+ info.id);
			getContentResolver().delete(uri, null, null);
			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, "DELETE");
    }

    private void createProduct() {
		Intent i = new Intent(this, ProductDetailActivity.class);
		startActivity(i);
	}

	// Opens the second activity if an entry is clicked
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        buttonsound();
        Uri productUri = Uri.parse(MyProductContentProvider.CONTENT_URI + "/" + id);
        editProduct(productUri);
    }

    private void editProduct(Uri productUri) {
		Intent i = new Intent(this, ProductDetailActivity.class);
		i.putExtra(MyProductContentProvider.CONTENT_ITEM_TYPE, productUri);
		startActivity(i);
	}

	private void fillData() {

		// Fields from the database (projection)
		// Must include the _id column for the adapter to work
		String[] from = new String[] { ProductTable.COLUMN_NAME , ProductTable.COLUMN_QUANTITY};
		// Fields on the UI to which we map
		int[] to = new int[] { R.id.label , R.id.quantity};

		getLoaderManager().initLoader(0, null, this);
		adapter = new SimpleCursorAdapter(this, R.layout.product_row, null, from, to, 0);

		setListAdapter(adapter);
	}

	// Creates a new loader after the initLoader () call
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = { ProductTable.COLUMN_ID, ProductTable.COLUMN_NAME ,ProductTable.COLUMN_QUANTITY};
		CursorLoader cursorLoader = new CursorLoader(this,
				MyProductContentProvider.CONTENT_URI, projection, null, null, null);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// data is not available anymore, delete reference
		adapter.swapCursor(null);
	}

    // ======================================================================================

    private void filltable() {
        insfile("500","Torpedo","0-99999-11111-0","Photon Torpedo to fire at ennemys");
        insfile("10","Dilithium","0-99999-11111-0","Dilithium cristals to run the ship");
        insfile("4","Shuttle","0-99999-11111-0","Space shuttle");
        insfile("1024","Crew","0-99999-11111-0","Active Crew on ship");
        insfile("8","Officers","0-99999-11111-0","Senior officer who can drive the ship");
        insfile("243","DaysOfFood","0-99999-11111-0","Days of food reserve for ship complement");
        insfile("1500","SpaceSuits","0-99999-11111-0","Space proof suit");
        insfile("47534","ToiletPaper","0-99999-11111-0","Space proof simple toilet paper rolls");
        insfile("12","Doctors","0-99999-11111-0","Doctors who can practice medecine");
        insfile("3","Consultant","0-99999-11111-0","Shrink");
        insfile("10","Antiproton Inhibitors","0-99999-11111-0","Transporter fuel containers");
        insfile("256","Escape Pods","0-99999-11111-0","Whatatatow");
        insfile("12","Vulcan Ale","0-99999-11111-0","OOps, Prohibited, but have a special allowance from Kirk.");
        insfile("1","Cardassian Dildo","0-99999-11111-0","For Sick Bay purposes. (Which?)");
        insfile("10","Federation flags","0-99999-11111-0","For ceremony purposes. (Like spock first death)");
        insfile("10000","Crew Uniforms","0-99999-11111-0","To Stay clean-cut");
        insfile("5","Worf's furnitures","0-99999-11111-0","To replace broken ones");
        insfile("128","Bathrooms","0-99999-11111-0","To use when needed");
        insfile("20","Pools","0-99999-11111-0","Recreation and Rescue");
        insfile("2500","Trycorder","0-99999-11111-0","Ship operation and evaluation controllers");
    }

    private void insfile(String qty,String name, String upc, String desc) {
        ContentValues values = new ContentValues();
        values.put(ProductTable.COLUMN_QUANTITY, qty);
        values.put(ProductTable.COLUMN_NAME, name);
        values.put(ProductTable.COLUMN_UPC, upc);
        values.put(ProductTable.COLUMN_DESCRIPTION, desc);
        Uri uri = getContentResolver().insert(MyProductContentProvider.CONTENT_URI, values);
    }


}