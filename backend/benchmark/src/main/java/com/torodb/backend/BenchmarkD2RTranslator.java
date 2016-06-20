package com.torodb.backend;

import com.torodb.backend.util.InMemoryRidGenerator;
import com.torodb.backend.util.TestDataFactory;
import com.torodb.core.TableRefFactory;
import com.torodb.core.d2r.D2RTranslator;
import com.torodb.core.impl.TableRefFactoryImpl;
import com.torodb.core.transaction.metainf.MetainfoRepository.SnapshotStage;
import com.torodb.core.transaction.metainf.MutableMetaSnapshot;
import com.torodb.kvdocument.values.KVDocument;
import com.torodb.metainfo.cache.mvcc.MvccMetainfoRepository;
import java.util.ArrayList;
import java.util.List;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import static com.torodb.backend.util.TestDataFactory.*;

public class BenchmarkD2RTranslator {

    private static TableRefFactory tableRefFactory = new TableRefFactoryImpl();
    private static InMemoryRidGenerator ridGenerator = new InMemoryRidGenerator();
	
	@State(Scope.Thread)
	public static class TranslateState {
		
		public List<KVDocument> document=new ArrayList<>();

		@Setup(Level.Invocation)
		public void setup(){
			document=new ArrayList<>();
			for (int i=0;i<100;i++){
				document.add(TestDataFactory.buildDoc());
			}
		}
	}
	

	@Benchmark
	@Fork(value=5)
	@BenchmarkMode(value=Mode.Throughput)
	@Warmup(iterations=3)
	@Measurement(iterations=10) 
	public void benchmarkTranslate(TranslateState state, Blackhole blackhole) {
		MvccMetainfoRepository mvccMetainfoRepository = new MvccMetainfoRepository(InitialView);
		MutableMetaSnapshot mutableSnapshot;
		try (SnapshotStage snapshot = mvccMetainfoRepository.startSnapshotStage()) {
			mutableSnapshot = snapshot.createMutableSnapshot();
		}
		D2RTranslator translator = new D2RTranslatorImpl(tableRefFactory, ridGenerator, mutableSnapshot.getMetaDatabaseByName(DB1), mutableSnapshot.getMetaDatabaseByName(DB1).getMetaCollectionByName(COLL1));
		for(KVDocument doc: state.document){
			translator.translate(doc);
		}
		blackhole.consume(translator.getCollectionDataAccumulator());
	}
	
}
