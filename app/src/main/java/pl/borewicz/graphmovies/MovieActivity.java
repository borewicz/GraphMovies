package pl.borewicz.graphmovies;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
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

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.StatementResult;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MovieActivity extends AppCompatActivity {

    //private Map<String, String> movieInfo;
    private List<String[]> movieInfo;
    String imageUrl, trailer, title;
    ArrayAdapter<String[]> adapter;
    RecyclerView recyclerView;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        movieInfo = new ArrayList<String[]>();

        setContentView(R.layout.activity_movie);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.att_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

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

    public class MovieAdapter extends RecyclerView.Adapter {
        private RecyclerView mRecyclerView;
        private List<String[]> mDataset;

        private class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView mTitle;
            public TextView mContent;

            public MyViewHolder(View pItem) {
                super(pItem);
                mTitle = (TextView) pItem.findViewById(R.id.article_title);
                mContent = (TextView) pItem.findViewById(R.id.article_subtitle);
            }
        }

        public MovieAdapter(List<String[]> pDataset, RecyclerView pRecyclerView) {
            mDataset = pDataset;
            mRecyclerView = pRecyclerView;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.movie_card, viewGroup, false);

            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int i) {
            String[] entry = mDataset.get(i);
            ((MyViewHolder) viewHolder).mTitle.setText(entry[0]);
            ((MyViewHolder) viewHolder).mContent.setText(entry[1]);
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
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

                //Map<String, Object> mapa = record.asMap();
                Node node = record.get("m").asNode();
                //movieInfo.add(new String[] { "Title", node.get("title").asString()});
                if (!node.get("tagline").asString().isEmpty())
                    movieInfo.add(new String[]{"Tagline", node.get("tagline").asString()});
                if (!node.get("studio").asString().isEmpty())
                    movieInfo.add(new String[]{"Studio", node.get("studio").asString()});
                movieInfo.add(new String[]{"Description", node.get("description").asString()});
                movieInfo.add(new String[]{"Language", node.get("language").asString()});
                movieInfo.add(new String[]{"Genre", node.get("genre").asString()});

                trailer = node.get("trailer").asString();
                imageUrl = node.get("imageUrl").asString();
                title = node.get("title").asString();
            }

            session.close();
            return null;
        }

        protected void onPostExecute(String file_url) {
            //pDialog.dismiss();
            runOnUiThread(new Runnable() {
                public void run() {
                    recyclerView.setAdapter(new MovieAdapter(movieInfo, recyclerView));
                    toolbar.setTitle(title);
                }
            });
        }
    }
}
