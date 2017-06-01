package pl.borewicz.graphmovies;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MovieActivity extends AppCompatActivity {

    //private Map<String, String> movieInfo;
    private List<String[]> movieInfo;
    String imageUrl, trailer;
    ArrayAdapter<String[]> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        movieInfo = new ArrayList<String[]>();

        setContentView(R.layout.activity_movie);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adapter = new ArrayAdapter<String[]>(this, android.R.layout.simple_list_item_2, android.R.id.text1, movieInfo) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                String[] entry = movieInfo.get(position);
                text1.setText(entry[0]);
                text2.setText(entry[1]);
                return view;
            }
        };
        ListView listView = (ListView) findViewById(R.id.att_list);
        listView.setOnTouchListener(new View.OnTouchListener() {
            // Setting on Touch Listener for handling the touch inside ScrollView
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Disallow the touch request for parent scroll on touch of child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        listView.setAdapter(adapter);

        String query = String.format(Locale.US, "MATCH (m:Movie) WHERE id(m)=%d return m", getIntent().getIntExtra("movie_id", 0));
        new LoadMovie(query).execute();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    class LoadMovie extends AsyncTask<String, String, String> {
        String mQuery;

        LoadMovie(String query) {
            mQuery = query;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... args) {
            Session session = Neo4jDriver.getInstance().getDriver().session();

            StatementResult result = session.run(mQuery);

            while (result.hasNext()) {
                Record record = result.next();
                //List<Object> stuff = record.get("m").asList();
                movieInfo.add(new String[] { "Title", record.get("title").asString() });
                movieInfo.add(new String[] { "Tagline", record.get("m.tagline").asString() });
                movieInfo.add(new String[] { "Studio", record.get("m.studio").asString() });
                movieInfo.add(new String[] { "Description", record.get("m.description").asString() });
                movieInfo.add(new String[] { "Language", record.get("m.language").asString() });
                movieInfo.add(new String[] { "Genre", record.get("m.genre").asString() });

                trailer =  record.get("m.genre").asString();
                imageUrl = record.get("m.imageUrl").asString();
            }

            session.close();
            return null;
        }

        protected void onPostExecute(String file_url) {
            //pDialog.dismiss();
            runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                    setListViewHeightBasedOnChildren((ListView) findViewById(R.id.att_list));
                }
            });
        }
    }
}
