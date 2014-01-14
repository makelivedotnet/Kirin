package com.futureplatforms.kirin.android.db;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.futureplatforms.kirin.dependencies.db.Database;
import com.futureplatforms.kirin.dependencies.db.DatabaseDelegate;
import com.futureplatforms.kirin.dependencies.db.Transaction.RowSet;
import com.futureplatforms.kirin.dependencies.db.Transaction.Statement;
import com.futureplatforms.kirin.dependencies.db.Transaction.StatementWithRowsReturn;
import com.futureplatforms.kirin.dependencies.db.Transaction.StatementWithTokenReturn;
import com.futureplatforms.kirin.dependencies.db.Transaction.TxElementType;
import com.futureplatforms.kirin.dependencies.db.Transaction.TxRowsCB;
import com.futureplatforms.kirin.dependencies.db.Transaction.TxTokenCB;
import com.futureplatforms.kirin.dependencies.internal.TransactionBackend;
import com.futureplatforms.kirin.dependencies.internal.TransactionBundle;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AndroidDatabase implements DatabaseDelegate {

	private class Helper extends SQLiteOpenHelper {
		public Helper(String name) {
			super(_Context, name, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	private final Context _Context;

	public AndroidDatabase(Context context) {
		this._Context = context;
	}

	private static class AndroidDatabaseImpl extends Database {

		private SQLiteDatabase db;

		public AndroidDatabaseImpl(SQLiteDatabase db) {
			this.db = db;
		}

		@Override
		protected void performTransaction(TransactionCallback cb) {
			cb.onSuccess(new TransactionBackend() {

				@Override
				public void pullTrigger(TransactionBundle bundle) {
					db.beginTransaction();
					try {

						int statementCount = 0, batchCount = 0;
						for (TxElementType type : bundle._Types) {
							if (type == TxElementType.Statement) {
								Statement st = bundle._Statements
										.get(statementCount);
								statementCount++;

								// Execute the statement
								Cursor cursor = db
										.rawQuery(st._SQL, st._Params);
								if (st instanceof StatementWithRowsReturn) {
									// rows return
									// construct RowSet from cursor
									ImmutableList<String> columnNames = ImmutableList
											.copyOf(cursor.getColumnNames());
									RowSet rowset = new RowSet(columnNames);
									int colCount = cursor.getColumnCount();
									while (cursor.moveToNext()) {
										List<String> values = Lists
												.newArrayList();
										for (int i = 0; i < colCount; i++) {
											// Everything has to be a string...
											int entryType = getType(cursor, i);
											switch (entryType) {
											case FIELD_TYPE_BLOB: {
												values.add(new String(cursor
														.getBlob(i)));
											}
												break;

											case FIELD_TYPE_FLOAT: {
												values.add(String
														.valueOf(cursor
																.getDouble(i)));
											}
												break;

											case FIELD_TYPE_INTEGER: {
												values.add(String
														.valueOf(cursor
																.getInt(i)));
											}
												break;

											case FIELD_TYPE_NULL: {
												values.add(null);
											}
												break;

											case FIELD_TYPE_STRING: {
												values.add(cursor.getString(i));
											}
												break;
											}
										}
										rowset.addRow(values);
									}
									TxRowsCB c = ((StatementWithRowsReturn) st)._Callback;
									if (c != null) {
										c.onSuccess(rowset);
									}
								} else {
									// token return -- stick it on the dropbox!!
									String token = AndroidDbDropbox
											.getInstance().putCursor(cursor);
									TxTokenCB c = ((StatementWithTokenReturn) st)._Callback;
									if (c != null) {
										c.onSuccess(token);
									}
								}
							} else {
								String[] batch = bundle._Batches
										.get(batchCount);
								for (String sql : batch) {
									db.execSQL(sql);
								}
								batchCount++;
							}
						}
						db.setTransactionSuccessful();
						db.endTransaction();
						bundle._ClosedCallback.onComplete();
					} catch (Exception e) {
						e.printStackTrace();
						db.endTransaction();
						bundle._ClosedCallback.onError();
					} 
				}
			});

		}
	}

	Map<String, SQLiteOpenHelper> dbHelperMap = Maps.newHashMap();

	@Override
	public void open(String filename, DatabaseOpenedCallback cb) {
		SQLiteOpenHelper helper = dbHelperMap.get(filename);
		if (helper == null) {
			helper = new Helper(filename);
			dbHelperMap.put(filename, helper);
		}
		SQLiteDatabase _db = helper.getWritableDatabase();

		cb.onOpened(new AndroidDatabaseImpl(_db));
	}

	protected static final int FIELD_TYPE_BLOB = 4;
	protected static final int FIELD_TYPE_FLOAT = 2;
	protected static final int FIELD_TYPE_INTEGER = 1;
	protected static final int FIELD_TYPE_NULL = 0;
	protected static final int FIELD_TYPE_STRING = 3;

	static int getType(Cursor cursor, int i) throws Exception {
		SQLiteCursor sqLiteCursor = (SQLiteCursor) cursor;
		CursorWindow cursorWindow = sqLiteCursor.getWindow();
		int pos = cursor.getPosition();
		int type = -1;
		if (cursorWindow.isNull(pos, i)) {
			type = FIELD_TYPE_NULL;
		} else if (cursorWindow.isLong(pos, i)) {
			type = FIELD_TYPE_INTEGER;
		} else if (cursorWindow.isFloat(pos, i)) {
			type = FIELD_TYPE_FLOAT;
		} else if (cursorWindow.isString(pos, i)) {
			type = FIELD_TYPE_STRING;
		} else if (cursorWindow.isBlob(pos, i)) {
			type = FIELD_TYPE_BLOB;
		}

		return type;
	}
}
