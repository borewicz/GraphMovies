package pl.borewicz.graphmovies;

import android.content.Intent;
import android.graphics.Movie;
import android.os.AsyncTask;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.json.JsonArray;
import javax.json.JsonObject;

class MovieItem {
    private String name;
    private String genre;
    private int id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

public class MainActivity extends AppCompatActivity {

    private List<MovieItem> moviesList;
    private boolean flag_loading = false, searchMode = false;
    private ArrayAdapter<MovieItem> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        moviesList = new ArrayList<MovieItem>();

        adapter = new ArrayAdapter<MovieItem>(this, android.R.layout.simple_list_item_2, android.R.id.text1, moviesList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(moviesList.get(position).getName());
                text2.setText(moviesList.get(position).getGenre());
                return view;
            }
        };
        ListView listView = (ListView) findViewById(R.id.movies_list);
        listView.setAdapter(adapter);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            public void onScrollStateChanged(AbsListView view, int scrollState) {


            }

            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

                if (firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount != 0) {
                    if (flag_loading == false && searchMode == false) {
                        flag_loading = true;
                        new LoadMovies(createQuery(moviesList.size()+1, 10)).execute();
                    }
                }
            }
        });
        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                MovieItem movie = (MovieItem) arg0.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, MovieActivity.class);
                intent.putExtra("movie_id", movie.getId());
                startActivity(intent);
            }
        });
        new LoadMovies(createQuery(0, 10)).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_view_menu_item, menu);
        MenuItem searchViewItem = menu.findItem(R.id.action_search);
        final SearchView searchViewAndroidActionBar = (SearchView) MenuItemCompat.getActionView(searchViewItem);
        searchViewAndroidActionBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchMode = true;
                searchViewAndroidActionBar.clearFocus();
                moviesList.clear();
                adapter.notifyDataSetChanged();
                new LoadMovies(String.format("MATCH (m:Movie) WHERE m.title CONTAINS '%s' return m.title,m.genre,id(m) as movie_id ORDER BY m.title", query)).execute();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchViewAndroidActionBar.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View arg0) {
                searchMode = false;
                moviesList.clear();
                new LoadMovies(createQuery(0, 10)).execute();
            }

            @Override
            public void onViewAttachedToWindow(View arg0) {
                // search was opened
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private String createQuery(int skip, int limit) {
        return String.format(Locale.US,
                "MATCH (m:Movie) RETURN m.title,m.genre,id(m) as movie_id ORDER BY m.title SKIP %d LIMIT %d", skip, limit);
    }

    class LoadMovies extends AsyncTask<String, String, String> {
        //int mSkip, mLimit;
        String mQuery;

        LoadMovies(String query) {
            mQuery = query;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... args) {
            //createQuery(mLimit, mSkip)
            try {
                JsonObject json = Neo4jDriver.sendPost(mQuery);

                for (JsonArray record : json.getJsonArray("data").getValuesAs(JsonArray.class)) {
                    MovieItem item = new MovieItem();
                    item.setName(record.getString(0));
                    try {
                        item.setGenre(record.getString(1));
                    }
                    catch (ClassCastException e) {
                        item.setGenre("Unknown");
                    }
                    item.setId(record.getInt(2));
                    moviesList.add(item);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String file_url) {
            //pDialog.dismiss();
            runOnUiThread(new Runnable() {
                public void run() {
                    flag_loading = false;
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }
}
