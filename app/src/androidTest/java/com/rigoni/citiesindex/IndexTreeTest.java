package com.rigoni.citiesindex;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.rigoni.citiesindex.data.City;
import com.rigoni.citiesindex.index.IndexTree;
import com.rigoni.citiesindex.index.IndexTreeEntry;
import com.rigoni.citiesindex.index.IndexTreeStorage;
import com.rigoni.citiesindex.index.IndexTreeStorageFs;
import com.rigoni.citiesindex.utils.FsUtils;
import com.rigoni.citiesindex.utils.NameNormalizer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class IndexTreeTest {

    private static final String TEST_LARGE_JSON = "[\n" +
            "{\"country\":\"UA\",\"name\":\"Hurzuf\",\"_id\":707860,\"coord\":{\"lon\":34.283333,\"lat\":44.549999}},\n" +
            "{\"country\":\"RU\",\"name\":\"Novinki\",\"_id\":519188,\"coord\":{\"lon\":37.666668,\"lat\":55.683334}},\n" +
            "{\"country\":\"NP\",\"name\":\"Gorkhā\",\"_id\":1283378,\"coord\":{\"lon\":84.633331,\"lat\":28}},\n" +
            "{\"country\":\"IN\",\"name\":\"State of Haryāna\",\"_id\":1270260,\"coord\":{\"lon\":76,\"lat\":29}},\n" +
            "{\"country\":\"UA\",\"name\":\"Holubynka\",\"_id\":708546,\"coord\":{\"lon\":33.900002,\"lat\":44.599998}},\n" +
            "{\"country\":\"NP\",\"name\":\"Bāgmatī Zone\",\"_id\":1283710,\"coord\":{\"lon\":85.416664,\"lat\":28}},\n" +
            "{\"country\":\"RU\",\"name\":\"Mar’ina Roshcha\",\"_id\":529334,\"coord\":{\"lon\":37.611111,\"lat\":55.796391}},\n" +
            "{\"country\":\"IN\",\"name\":\"Republic of India\",\"_id\":1269750,\"coord\":{\"lon\":77,\"lat\":20}},\n" +
            "{\"country\":\"NP\",\"name\":\"Kathmandu\",\"_id\":1283240,\"coord\":{\"lon\":85.316666,\"lat\":27.716667}},\n" +
            "{\"country\":\"UA\",\"name\":\"Laspi\",\"_id\":703363,\"coord\":{\"lon\":33.733334,\"lat\":44.416668}},\n" +
            "{\"country\":\"VE\",\"name\":\"Merida\",\"_id\":3632308,\"coord\":{\"lon\":-71.144997,\"lat\":8.598333}},\n" +
            "{\"country\":\"RU\",\"name\":\"Vinogradovo\",\"_id\":473537,\"coord\":{\"lon\":38.545555,\"lat\":55.423332}},\n" +
            "{\"country\":\"IQ\",\"name\":\"Qarah Gawl al ‘Ulyā\",\"_id\":384848,\"coord\":{\"lon\":45.6325,\"lat\":35.353889}},\n" +
            "{\"country\":\"RU\",\"name\":\"Cherkizovo\",\"_id\":569143,\"coord\":{\"lon\":37.728889,\"lat\":55.800835}},\n" +
            "{\"country\":\"UA\",\"name\":\"Alupka\",\"_id\":713514,\"coord\":{\"lon\":34.049999,\"lat\":44.416668}},\n" +
            "{\"country\":\"DE\",\"name\":\"Lichtenrade\",\"_id\":2878044,\"coord\":{\"lon\":13.40637,\"lat\":52.398441}},\n" +
            "{\"country\":\"RU\",\"name\":\"Zavety Il’icha\",\"_id\":464176,\"coord\":{\"lon\":37.849998,\"lat\":56.049999}},\n" +
            "{\"country\":\"IL\",\"name\":\"‘Azriqam\",\"_id\":295582,\"coord\":{\"lon\":34.700001,\"lat\":31.75}},\n" +
            "{\"country\":\"IN\",\"name\":\"Ghūra\",\"_id\":1271231,\"coord\":{\"lon\":79.883331,\"lat\":24.766666}},\n" +
            "{\"country\":\"UA\",\"name\":\"Tyuzler\",\"_id\":690856,\"coord\":{\"lon\":34.083332,\"lat\":44.466667}},\n" +
            "{\"country\":\"RU\",\"name\":\"Zaponor’ye\",\"_id\":464737,\"coord\":{\"lon\":38.861942,\"lat\":55.639999}},\n" +
            "{\"country\":\"UA\",\"name\":\"Il’ichëvka\",\"_id\":707716,\"coord\":{\"lon\":34.383331,\"lat\":44.666668}},\n" +
            "{\"country\":\"UA\",\"name\":\"Partyzans’ke\",\"_id\":697959,\"coord\":{\"lon\":34.083332,\"lat\":44.833332}},\n" +
            "{\"country\":\"RU\",\"name\":\"Yurevichi\",\"_id\":803611,\"coord\":{\"lon\":39.934444,\"lat\":43.600555}},\n" +
            "{\"country\":\"GE\",\"name\":\"Gumist’a\",\"_id\":614371,\"coord\":{\"lon\":40.973888,\"lat\":43.026943}},\n" +
            "{\"country\":\"GE\",\"name\":\"Ptitsefabrika\",\"_id\":874560,\"coord\":{\"lon\":40.290558,\"lat\":43.183613}},\n" +
            "{\"country\":\"GE\",\"name\":\"Orekhovo\",\"_id\":874652,\"coord\":{\"lon\":40.146111,\"lat\":43.351391}},\n" +
            "{\"country\":\"NG\",\"name\":\"Birim\",\"_id\":2347078,\"coord\":{\"lon\":9.997027,\"lat\":10.062094}},\n" +
            "{\"country\":\"RU\",\"name\":\"Priiskovyy\",\"_id\":2051302,\"coord\":{\"lon\":132.822495,\"lat\":42.819168}},\n" +
            "{\"country\":\"RU\",\"name\":\"Dzhaga\",\"_id\":563692,\"coord\":{\"lon\":42.650002,\"lat\":43.25}},\n" +
            "{\"country\":\"RU\",\"name\":\"Tret’ya Rota\",\"_id\":481725,\"coord\":{\"lon\":39.681389,\"lat\":43.741943}},\n" +
            "{\"country\":\"GB\",\"name\":\"Ruislip\",\"_id\":2638976,\"coord\":{\"lon\":-0.42341,\"lat\":51.573441}},\n" +
            "{\"country\":\"DE\",\"name\":\"Karow\",\"_id\":2892705,\"coord\":{\"lon\":13.48117,\"lat\":52.609039}},\n" +
            "{\"country\":\"DE\",\"name\":\"Gatow\",\"_id\":2922336,\"coord\":{\"lon\":13.18285,\"lat\":52.483238}},\n" +
            "{\"country\":\"ZA\",\"name\":\"Mkuze\",\"_id\":975511,\"coord\":{\"lon\":32.038609,\"lat\":-27.616409}},\n" +
            "{\"country\":\"CN\",\"name\":\"Lhasa\",\"_id\":1280737,\"coord\":{\"lon\":91.099998,\"lat\":29.65}},\n" +
            "{\"country\":\"TR\",\"name\":\"İstanbul\",\"_id\":745042,\"coord\":{\"lon\":28.983311,\"lat\":41.03508}}]";

    private static final String TEST_LARGE_JSON_SORTED =
            "[{\"country\":\"US\",\"name\":\"Downs\",\"_id\":4270472,\"coord\":{\"lon\":-98.542007,\"lat\":39.498619}},\n" +
            "{\"country\":\"US\",\"name\":\"Downs\",\"_id\":4890138,\"coord\":{\"lon\":-88.870628,\"lat\":40.39698}},\n" +
            "{\"country\":\"CA\",\"name\":\"Downsview\",\"_id\":5942354,\"coord\":{\"lon\":-79.48291,\"lat\":43.716808}},\n" +
            "{\"country\":\"CA\",\"name\":\"Downsview\",\"_id\":7871305,\"coord\":{\"lon\":-79.49398,\"lat\":43.732029}},\n" +
            "{\"country\":\"GB\",\"name\":\"Downton\",\"_id\":2651019,\"coord\":{\"lon\":-2.83333,\"lat\":52.366669}},\n" +
            "{\"country\":\"GB\",\"name\":\"Downton\",\"_id\":2651020,\"coord\":{\"lon\":-1.73333,\"lat\":51}},\n" +
            "{\"country\":\"CA\",\"name\":\"Downtown Toronto\",\"_id\":6167863,\"coord\":{\"lon\":-79.382896,\"lat\":43.650108}},\n" +
            "{\"country\":\"IE\",\"name\":\"Dowra\",\"_id\":2964677,\"coord\":{\"lon\":-8.015,\"lat\":54.191109}},\n" +
            "{\"country\":\"AF\",\"name\":\"Dowr-e Rabat\",\"_id\":1145364,\"coord\":{\"lon\":68.790932,\"lat\":36.691319}},\n" +
            "{\"country\":\"AU\",\"name\":\"Dows Creek\",\"_id\":2168424,\"coord\":{\"lon\":148.783325,\"lat\":-21.1}},\n" +
            "{\"country\":\"BY\",\"name\":\"Dowsk\",\"_id\":628923,\"coord\":{\"lon\":30.4601,\"lat\":53.157101}},\n" +
            "{\"country\":\"MX\",\"name\":\"Doxey\",\"_id\":3529689,\"coord\":{\"lon\":-99.23333,\"lat\":20.08333}},\n" +
            "{\"country\":\"ID\",\"name\":\"Doya\",\"_id\":7570125,\"coord\":{\"lon\":121.162598,\"lat\":-8.8234}},\n" +
            "{\"country\":\"FR\",\"name\":\"Doyet\",\"_id\":3020867,\"coord\":{\"lon\":2.79707,\"lat\":46.334759}},\n" +
            "{\"country\":\"FR\",\"name\":\"Doyet\",\"_id\":6425346,\"coord\":{\"lon\":2.8,\"lat\":46.333328}},\n" +
            "{\"country\":\"AU\",\"name\":\"Doyles Creek\",\"_id\":2168419,\"coord\":{\"lon\":150.783325,\"lat\":-32.533329}},\n" +
            "{\"country\":\"US\",\"name\":\"Doylestown\",\"_id\":5152278,\"coord\":{\"lon\":-81.696518,\"lat\":40.970051}},\n" +
            "{\"country\":\"US\",\"name\":\"Doylestown\",\"_id\":5187247,\"coord\":{\"lon\":-75.12989,\"lat\":40.310108}},\n" +
            "{\"country\":\"US\",\"name\":\"Doylestown\",\"_id\":5187248,\"coord\":{\"lon\":-77.721382,\"lat\":40.197578}},\n" +
            "{\"country\":\"US\",\"name\":\"Doyleville\",\"_id\":5419932,\"coord\":{\"lon\":-106.609482,\"lat\":38.45166}},\n" +
            "{\"country\":\"ID\",\"name\":\"Doyok\",\"_id\":8058070,\"coord\":{\"lon\":111.249397,\"lat\":-6.7873}},\n" +
            "{\"country\":\"PH\",\"name\":\"Doyong Malabago\",\"_id\":1714353,\"coord\":{\"lon\":120.351501,\"lat\":15.9675}},\n" +
            "{\"country\":\"US\",\"name\":\"Doyon\",\"_id\":5058910,\"coord\":{\"lon\":-98.536774,\"lat\":48.05278}},\n" +
            "{\"country\":\"US\",\"name\":\"Dozier\",\"_id\":4059229,\"coord\":{\"lon\":-86.36496,\"lat\":31.492109}}]";

    private Context mContext;
    private File mIndexDirectory;
    private IndexTreeStorageFs mIndexTreeStorage;

    private IndexTreeStorage mMockIndexTreeStorage;

    @Before
    public void setUp() throws Exception {
        mMockIndexTreeStorage = mock(IndexTreeStorageFs.class);

        mContext = InstrumentationRegistry.getTargetContext();

        mIndexDirectory = new File(mContext.getCacheDir() + File.separator + "test_index");
        mIndexTreeStorage = new IndexTreeStorageFs<City>(City.class, mIndexDirectory.getAbsolutePath(), true);
    }

    @After
    public void tearDown() {
        FsUtils.deleteDirectory(mIndexDirectory);
    }

    @Test
    public void testAddToCitiesSameSubpath() {
        // Given an index tree backed by a mock storage
        final IndexTree indexTree = new IndexTree(mMockIndexTreeStorage);
        final City firstCity = mockCity("Amsterdam");
        final City secondCity = mockCity("Amstelveen");
        final String expectedSubPath = "a/m/s/";

        // When more cities are added
        indexTree.addEntry(firstCity);
        indexTree.addEntry(secondCity);

        // Then the mock storage should receive calls to add corresponding entries
        verify(mMockIndexTreeStorage).addEntryAtSubPath(expectedSubPath, firstCity);
        verify(mMockIndexTreeStorage).addEntryAtSubPath(expectedSubPath, secondCity);
    }

    @Test
    public void testFilterForwardSingleCharacterThreeResultsExpected() {
        // Given an index tree with a known list of cities
        final IndexTree indexTree = new IndexTree(mIndexTreeStorage);
        addEntriesFromJson(TEST_LARGE_JSON, indexTree);

        // When a filter is requested with one character
        List<IndexTreeEntry> result = indexTree.filterForward("p", "", 100);

        // Then three cities should be returned
        assertEquals(3, result.size());
        assertEquals("Partyzans’ke", ((City)result.get(0)).getName());
        assertEquals("Ptitsefabrika", ((City)result.get(1)).getName());
        assertEquals("Priiskovyy", ((City)result.get(2)).getName());
    }

    @Test
    public void testFilterForwardSingleCharacterWithApostrophe() {
        // Given an index tree with a known list of cities
        final IndexTree indexTree = new IndexTree(mIndexTreeStorage);
        addEntriesFromJson(TEST_LARGE_JSON, indexTree);

        // When a filter is requested with an apostrophe
        List<IndexTreeEntry> result = indexTree.filterForward("‘", "", 100);

        // Then only one city should be returned
        assertEquals(1, result.size());
        assertEquals("‘Azriqam", ((City)result.get(0)).getName());
    }

    @Test
    public void testFilterForwardTwoCharsManyResultsExpected() {
        // Given an index tree with a known list of cities
        final IndexTree indexTree = new IndexTree(mIndexTreeStorage);
        addEntriesFromJson(TEST_LARGE_JSON_SORTED, indexTree);

        // When a filter is requested with two characters
        List<IndexTreeEntry> result = indexTree.filterForward("do", "", 100);

        // Then only the expected amount of cities should be returned
        assertEquals(24, result.size());
    }

    @Test
    public void testFilterForwardThreeCharsManyResultsExpected() {
        // Given an index tree with a known list of cities
        final IndexTree indexTree = new IndexTree(mIndexTreeStorage);
        addEntriesFromJson(TEST_LARGE_JSON_SORTED, indexTree);

        // When a filter is requested with three characters
        List<IndexTreeEntry> result = indexTree.filterForward("dow", "", 100);

        // Then only the expected amount of cities should be returned
        assertEquals(11, result.size());
    }

    @Test
    public void testFilterForwardFourCharsManyResultsExpected() {
        // Given an index tree with a known list of cities
        final IndexTree indexTree = new IndexTree(mIndexTreeStorage);
        addEntriesFromJson(TEST_LARGE_JSON_SORTED, indexTree);

        // When more cities then available, starting with a prefix, are requested
        List<IndexTreeEntry> result = indexTree.filterForward("doyl", "", 100);

        // Then only the expected amount of cities should be returned
        assertEquals(5, result.size());
    }

    @Test
    public void testCitiesWithSaintPrefix() throws IOException {
        // Given an index tree with a known list of cities
        final IndexTree indexTree = new IndexTree(mIndexTreeStorage);
        final InputStream is = mContext.getResources().getAssets().open("cities_prefix_sain.json");
        addEntriesFromInputStream(is, indexTree);
        is.close();

        // When more cities then available, starting with "saint", are requested
        List<IndexTreeEntry> result = indexTree.filterForward("saint", "", 400);

        // Then the cities starting with "saint" should match the known amount
        assertEquals(382, result.size());
    }

    @Test
    public void testCitiesWithSaintDashPrefix() throws IOException {
        // Given an index tree with a known list of cities
        final IndexTree indexTree = new IndexTree(mIndexTreeStorage);
        final InputStream is = mContext.getResources().getAssets().open("cities_prefix_sain.json");
        addEntriesFromInputStream(is, indexTree);
        is.close();

        // When more cities then available, starting with "saint", are requested
        List<IndexTreeEntry> result = indexTree.filterForward("saint-", "", 400);

        // Then the cities starting with "saint-" should match the known amount
        assertEquals(348, result.size());
    }

    private void addEntriesFromJson(final String json, final IndexTree tree) {
        JsonElement element = new JsonParser().parse(json);
        JsonArray array = element.getAsJsonArray();
        final Gson gson = new GsonBuilder().create();

        for (int i = 0; i < array.size(); i++) {
            final City city = gson.fromJson(array.get(i), City.class);
            tree.addEntry(city);
        }
    }

    public void addEntriesFromInputStream(final InputStream is, final IndexTree tree) throws IOException
    {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is), 4096);
        String line;
        StringBuilder sb =  new StringBuilder();
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        addEntriesFromJson(sb.toString(), tree);
    }

    private City mockCity(@NonNull final String name) {
        final City city = mock(City.class);
        when(city.getIndexTreeKey()).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return new NameNormalizer().normalize(name);
            }
        });
        return city;
    }
}
