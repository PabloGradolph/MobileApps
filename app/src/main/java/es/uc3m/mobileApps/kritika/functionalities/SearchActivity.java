package es.uc3m.mobileApps.kritika.functionalities;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.uc3m.mobileApps.kritika.DashboardUserActivity;
import es.uc3m.mobileApps.kritika.R;
import es.uc3m.mobileApps.kritika.model.Book;
import es.uc3m.mobileApps.kritika.model.Movie;
import es.uc3m.mobileApps.kritika.model.SearchInterface;
import es.uc3m.mobileApps.kritika.model.Song;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchActivity extends DashboardUserActivity {

    private EditText searchBar;
    private Spinner contentTypeSpinner;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SearchAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // layout
        setContentView(R.layout.search_activity);

        // dropdown
        setupSpinner();

        // buttons
        Button buttonOpenMovies = findViewById(R.id.button_open_movies);
        Button buttonOpenMusic = findViewById(R.id.button_open_music);
        Button buttonOpenBooks = findViewById(R.id.button_open_books);
        ImageButton buttonOpenProfile = findViewById(R.id.profileButton);
        ImageButton buttonOpenHome = findViewById(R.id.houseButton);
        ImageButton buttonOpenSearch = findViewById(R.id.searchButton);

        // Set click listeners for buttons
        setButtonListeners(buttonOpenMovies, buttonOpenMusic, buttonOpenBooks, buttonOpenProfile,
                buttonOpenHome, buttonOpenSearch);

        // vistas
        searchBar = findViewById(R.id.searchBar);
        contentTypeSpinner = findViewById(R.id.contentTypeSpinner);
        recyclerView = findViewById(R.id.rvSearchResults);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);




        // search functionality
        /* para busqueda en real-time
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No es necesario implementar
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No es necesario implementar
            }

            @Override
            public void afterTextChanged(Editable s) {
                String searchQuery = s.toString();
                if (!searchQuery.isEmpty()) {
                    performSearch(searchQuery, contentTypeSpinner.getSelectedItem().toString());
                }
            }
        });
        */
        // Search functionality
        ImageButton buttonSearch = findViewById(R.id.buttonSearch);

        // Establece un OnClickListener al botón de búsqueda
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtén el texto de la barra de búsqueda y el tipo de contenido seleccionado
                String searchQuery = searchBar.getText().toString().trim();
                String contentType = contentTypeSpinner.getSelectedItem().toString();

                // Verifica que el campo de búsqueda no esté vacío antes de realizar la búsqueda
                if (!searchQuery.isEmpty()) {
                    performSearch(searchQuery, contentType);
                } else {
                    Toast.makeText(SearchActivity.this, "Please enter a search term", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }



    private void setupSpinner() {
        Spinner spinner = findViewById(R.id.contentTypeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.content_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Search in Firebase
    private void performSearch(String query, String type) {
        // convert type of media ("movies", "songs", "books") to name of the collection
        String collectionPath = type.toLowerCase();

        // prepare dynamic search query
        Query searchQuery = db.collection(collectionPath)
                .orderBy("title")
                .startAt(query)
                .endAt(query + "\uf8ff") // \uf8ff es un punto de código muy alto en el rango de Unicode, por lo que la consulta incluirá todos los campos que comiencen con 'query'
                .limit(10);
        // Execute search query
        searchQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                // actualizar un RecyclerView con los resultados
                updateUIWithResults(documents);
            } else {
                Toast.makeText(SearchActivity.this, "Search failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    // Update layout
    private void updateUIWithResults(List<DocumentSnapshot> results) {
        List<SearchInterface> searchables = new ArrayList<>();
        for (DocumentSnapshot document : results) {
            // in each doc search for 'type' so we know it is a 'movie', 'song', or 'book'
            String type = document.getString("type");
            if (type != null) {
                switch (type) {
                    case "movies":
                        Movie movie = document.toObject(Movie.class);
                        fetchAdditionalMovieDetails(movie); // Llama a la API para detalles adicionales
                        searchables.add(movie);
                        break;
                    case "songs":
                        Song song = document.toObject(Song.class);
                        fetchAdditionalSongDetails(song);
                        searchables.add(song);
                        break;
                    case "books":
                        Book book = document.toObject(Book.class);
                        fetchAdditionalBookDetails(book);
                        searchables.add(book);
                        break;
                    default:
                        break;
                }
            }
        }

        adapter.updateData(searchables);
    }

    // Movies API
    private void fetchAdditionalMovieDetails(Movie movie) {
        OkHttpClient client = new OkHttpClient();
        String baseUrl = "https://api.themoviedb.org/3/movie/";
        String token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI1YzBmM2FmNjcyNjM5YTJjZmUyNmY4NDMyMjk5NjNmNCIsInN1YiI6IjY1ZDg5ZjZiMTQ5NTY1MDE2MmY1YTZhNCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.Io4x374YopHoiG57NIBLZEroKn2vInK1Dzfddkp-ECE";
        Request request = new Request.Builder()
                .url(baseUrl + movie.getId()) // Asegúrate de que movie.getId() obtiene el ID correcto del objeto Movie
                .get()
                .addHeader("accept", "application/json")
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MovieDetail", "Error fetching movie details", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                String jsonData = response.body().string();
                try {
                    JSONObject movieJson = new JSONObject(jsonData);
                    // Asumiendo que tienes un método en Movie para actualizar los datos
                    movie.updateDetails(
                            movieJson.getString("title"),
                            movieJson.getString("overview"),
                            "https://image.tmdb.org/t/p/w500" + movieJson.getString("poster_path"),
                            movieJson.getString("vote_average"),
                            movieJson.getString("release_date")
                    );

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    Log.e("MovieDetail", "JSON parsing error", e);
                }
            }
        });
    }

    // Songs API
    private void fetchAdditionalSongDetails(Song song) {
        OkHttpClient client = new OkHttpClient();
        String tokenUrl = "https://accounts.spotify.com/api/token";
        String credentials = Base64.encodeToString(("904e4d28994c4a70963a2fb5b5744729:fb988307f5fd400fb34e8400fd557ca8").getBytes(), Base64.NO_WRAP);

        Request tokenRequest = new Request.Builder()
                .url(tokenUrl)
                .post(new FormBody.Builder().add("grant_type", "client_credentials").build())
                .addHeader("Authorization", "Basic " + credentials)
                .build();

        client.newCall(tokenRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SongDetail", "Error fetching song details", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                try {
                    Response tokenResponse = client.newCall(tokenRequest).execute();
                    String jsonData = tokenResponse.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);
                    String accessToken = jsonObject.getString("access_token");

                    String trackUrl = "https://api.spotify.com/v1/tracks/"+song.getId();

                    Request trackRequest = new Request.Builder()
                            .url(trackUrl)
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .build();

                    Response tracksResponse = client.newCall(trackRequest).execute();
                    String trackjsonData = tracksResponse.body().string();
                    Log.i("Json", String.valueOf(trackjsonData));
                    JSONObject songJson = new JSONObject(trackjsonData);
                    Log.i("Json", String.valueOf(songJson));

                    song.updateDetails(
                            songJson.getString("name"),
                            songJson.getJSONArray("artists").getJSONObject(0).getString("name"),
                            songJson.getJSONObject("external_urls").getString("spotify"),
                            songJson.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url")
                    );

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    Log.e("SongDetail", "JSON parsing error", e);
                }
            }
        });
    }

    // Books API
    private void fetchAdditionalBookDetails(Book book) {
        Log.i("BOOK", String.valueOf(book));
        OkHttpClient client = new OkHttpClient();
        String baseUrl = "https://www.googleapis.com/books/v1/volumes/";
        String apiKey = "AIzaSyAYXAuFSEO31onyneSK__KfxiYEdyyhIaA";
        Request request = new Request.Builder()
                .url(baseUrl + book.getId() + "?key=" + apiKey)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("BookDetail", "Error fetching book details", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                try {
                    Response booksResponse = client.newCall(request).execute();
                    String jsonData = booksResponse.body().string();
                    Log.i("Json", String.valueOf(jsonData));
                    JSONObject bookJson = new JSONObject(jsonData);
                    Log.i("Json", String.valueOf(bookJson));

                    JSONArray authorsJsonArray = bookJson.getJSONObject("volumeInfo").optJSONArray("authors");
                    List<String> authors = new ArrayList<>();
                    if (authorsJsonArray != null) {
                        for (int j = 0; j < authorsJsonArray.length(); j++) {
                            authors.add(authorsJsonArray.getString(j));
                        }
                    }

                    // Asumiendo que tienes un método en Movie para actualizar los datos
                    book.updateDetails(
                            bookJson.getJSONObject("volumeInfo").getString("title"),
                            authors,
                            bookJson.getJSONObject("volumeInfo").optString("publisher", "N/A"),
                            bookJson.getJSONObject("volumeInfo").optString("publishedDate", "N/A"),
                            bookJson.getJSONObject("volumeInfo").optString("description", "No description available."),
                            bookJson.getJSONObject("volumeInfo").getJSONObject("imageLinks").optString("thumbnail", "").replace("http://", "https://")
                    );

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    Log.e("BookDetail", "JSON parsing error", e);
                }
            }
        });
    }


}

