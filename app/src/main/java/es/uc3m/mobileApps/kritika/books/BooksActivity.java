package es.uc3m.mobileApps.kritika.books;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import es.uc3m.mobileApps.kritika.DashboardUserActivity;
import es.uc3m.mobileApps.kritika.R;
import es.uc3m.mobileApps.kritika.model.Book;
import es.uc3m.mobileApps.kritika.Misc.ApiConstants;

import es.uc3m.mobileApps.kritika.model.Song;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BooksActivity extends DashboardUserActivity {
    private RecyclerView rvBooks;
    private BooksAdapter adapter;
    private List<Book> bookList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_books);

        rvBooks = findViewById(R.id.rvBooks);
        rvBooks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BooksAdapter(this, bookList);
        //Funcionalidad para hacer click
        adapter.setOnItemClickListener(new BooksAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Book book) {
                Intent intent = new Intent(BooksActivity.this,BooksDetailActivity.class);
                intent.putExtra("title", book.getTitle());
                startActivity(intent);
            }
        });
        rvBooks.setAdapter(adapter);

        Button buttonOpenMovies = findViewById(R.id.button_open_movies);
        Button buttonOpenMusic = findViewById(R.id.button_open_music);
        Button buttonOpenBooks = findViewById(R.id.button_open_books);
        ImageButton buttonOpenProfile = findViewById(R.id.profileButton);
        ImageButton buttonOpenHome = findViewById(R.id.houseButton);
        ImageButton buttonOpenSearch = findViewById(R.id.searchButton);

        // Set click listeners for buttons
        setButtonListeners(buttonOpenMovies, buttonOpenMusic, buttonOpenBooks, buttonOpenProfile,
                buttonOpenHome, buttonOpenSearch);
        new DiscoverBooksTask().execute();
    }

    private class DiscoverBooksTask extends AsyncTask<Void, Void, List<Book>> {
        @Override
        protected List<Book> doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();
            List<Book> books = new ArrayList<>();
            String apiKey = ApiConstants.GOOGLE_BOOKS_API_KEY;
            String baseUrl = ApiConstants.GOOGLE_BOOKS_BESTSELLERS_URL + apiKey;

            Request request = new Request.Builder()
                    .url(baseUrl)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                String jsonData = response.body().string();
                JSONObject jsonObject = new JSONObject(jsonData);
                JSONArray items = jsonObject.getJSONArray("items");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject volumeInfo = item.getJSONObject("volumeInfo");
                    JSONArray authorsJsonArray = volumeInfo.optJSONArray("authors");
                    List<String> authors = new ArrayList<>();
                    if (authorsJsonArray != null) {
                        for (int j = 0; j < authorsJsonArray.length(); j++) {
                            authors.add(authorsJsonArray.getString(j));
                        }
                    }
                    String title = volumeInfo.getString("title");
                    String publisher = volumeInfo.optString("publisher", "N/A");
                    String publishedDate = volumeInfo.optString("publishedDate", "N/A");
                    String description = volumeInfo.optString("description", "No description available.");
                    String thumbnail = volumeInfo.getJSONObject("imageLinks").optString("thumbnail", "");
                    thumbnail = thumbnail.replace("http://", "https://");

                    books.add(new Book(item.getString("id"), title, authors, publisher, publishedDate, description, thumbnail));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return books;
        }

        @Override
        protected void onPostExecute(List<Book> books) {
            super.onPostExecute(books);
            if (!books.isEmpty()) {
                bookList.clear();
                bookList.addAll(books);
                adapter.notifyDataSetChanged();

                // Guardar las películas en Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                CollectionReference booksRef = db.collection("books");

                for (Book book : books) {
                    // Comprobar si el documento existe en Firestore basado en su ID
                    DocumentReference docRef = booksRef.document(String.valueOf(book.getId()));
                    docRef.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (!document.exists()) {
                                booksRef.document(String.valueOf(book.getId())).set(book);
                                Log.d(TAG, "The document for book with ID: " + book.getId() + " added.");
                            } else {
                                Log.d(TAG, "The document exists for book with ID: " + book.getId());
                            }
                        } else {
                            Log.d(TAG, "Error getting document: ", task.getException());
                        }
                    });
                }
            } else {
                Toast.makeText(BooksActivity.this, "Failed to fetch books data!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
