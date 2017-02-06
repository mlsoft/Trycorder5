package net.ddns.mlsoftlaberge.trycorder.products;

/*
*  By Martin Laberge (mlsoft), From March 2016 to november 2016.
*  Licence: Can be shared with anyone, for non profit, provided my name stays in the comments.
*  This is a conglomerate of examples codes found in differents public forums on internet.
*  I just used the public knowledge to fit a special way to use an android phone functions.
*/

/* Copyright 2016 Martin Laberge
*
*        Licensed under the Apache License, Version 2.0 (the "License");
*        you may not use this file except in compliance with the License.
*        You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*        Unless required by applicable law or agreed to in writing, software
*        distributed under the License is distributed on an "AS IS" BASIS,
*        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*        See the License for the specific language governing permissions and
*        limitations under the License.
*/

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class ProductTable {

	// Database table
	public static final String TABLE_PRODUCT = "product";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_QUANTITY = "quantity";
	public static final String COLUMN_UPC = "upc";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_DESCRIPTION = "description";

	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " 
			+ TABLE_PRODUCT
			+ "(" + COLUMN_ID + " integer primary key autoincrement, "
			+ COLUMN_QUANTITY + " text not null, "
			+ COLUMN_UPC + " text not null, "
			+ COLUMN_NAME + " text not null,"
			+ COLUMN_DESCRIPTION + " text not null"
			+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(ProductTable.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCT);
		onCreate(database);
	}
}
