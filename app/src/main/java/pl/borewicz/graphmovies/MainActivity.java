package pl.borewicz.graphmovies;

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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.neo4j.driver.v1.*;

class MovieItem {
    private String name;
    private String genre;

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
                        new LoadMovies(createQuery(moviesList.size(), 10)).execute();
                    }
                }
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
                new LoadMovies(String.format("MATCH (m:Movie) WHERE m.title CONTAINS '%s' return m.title,m.genre ORDER BY m.title", query)).execute();
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
                "MATCH (m:Movie) RETURN m.title,m.genre ORDER BY m.title SKIP %d LIMIT %d", skip, limit);
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
            Session session = Neo4jDriver.getInstance().getDriver().session();

            //createQuery(mLimit, mSkip)
            StatementResult result = session.run(mQuery);

            while (result.hasNext()) {
                Record record = result.next();
                System.out.println(record.get("title") + " " + record.get("name").asString());
                MovieItem item = new MovieItem();
                item.setName(record.get("m.title").asString());
                item.setGenre(record.get("m.genre").asString());
                moviesList.add(item);
            }

            session.close();
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
