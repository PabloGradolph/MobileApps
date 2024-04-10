package es.uc3m.mobileApps.kritika.newDashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import es.uc3m.mobileApps.kritika.R;
import es.uc3m.mobileApps.kritika.model.Book;

public class NewBooksAdapter extends RecyclerView.Adapter<NewBooksAdapter.BookViewHolder> {
    private List<Book> books;
    private LayoutInflater inflater;

    public NewBooksAdapter(Context context, List<Book> books) {
        this.inflater = LayoutInflater.from(context);
        this.books = books;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.new_book_item, parent, false);
        return new BookViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book currentBook = books.get(position);

        // Cargar imagen de portada con Glide
        Glide.with(holder.bookImageViewPoster.getContext())
                .load(currentBook.getThumbnail())
                .into(holder.bookImageViewPoster);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {

        final ImageView bookImageViewPoster;

        BookViewHolder(View itemView) {
            super(itemView);

            bookImageViewPoster = itemView.findViewById(R.id.bookImageViewPoster);
        }
    }
}

