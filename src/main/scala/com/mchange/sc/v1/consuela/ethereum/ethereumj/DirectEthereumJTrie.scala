package com.mchange.sc.v1.consuela.ethereum.ethereumj;

import com.mchange.sc.v1.consuela.ethereum._;
import com.mchange.sc.v1.consuela.ethereum.trie._;
import com.mchange.sc.v1.consuela.trie._;
import org.ethereum.datasource.KeyValueDataSource;

import scala.Iterable;
import scala.collection._;

object DirectEthereumJTrie {
  type EthNode = EmbeddableEthStylePMTrie.Node[Nibble,Seq[Byte],EthHash]

  trait InnerDb extends EthTrieDb with PMTrie.Database.BulkWriting[EthNode,EthHash] {
    def commit() : Unit;
    def rollback() : Unit;
  }
  class CachingDb( kvds : KeyValueDataSource ) extends InnerDb {
    val cache = concurrent.TrieMap.empty[EthHash,Node];
    val direct = new DirectDb( kvds );

    def put( hash : EthHash, node : Node ) : Unit = cache += ( hash -> node )
    def apply( hash : EthHash ) : Node = cache.getOrElse( hash, direct.apply( hash ) )

    def put( nodes : immutable.Map[EthHash,EthNode] ) : Unit = cache ++= nodes;

    def commit() : Unit = {
      val snapshot = cache.snapshot.toMap;
      direct.put( snapshot );
      cache --= snapshot.keys;
    }
    def rollback() : Unit = cache.clear();
  }
  class DirectDb( kvds : KeyValueDataSource ) extends InnerDb {
    def put( hash : EthHash, node : Node ) : Unit = kvds.put( hash.toByteArray, toRLP( node ).toArray );
    def apply( hash : EthHash ) : Node = {
      val nodeBytes = kvds.get( hash.toByteArray );
      fromRLP( nodeBytes )
    }
    def put( nodes : immutable.Map[EthHash,EthNode] ) : Unit = {

      // this is a dangerous construction, as Java byte[] is not a good hash key,
      // we are hashing by identity. but it should work here, and is required by
      // the org.ethereum.datasource.KeyValueDataSource
      val bulkmap = new java.util.HashMap[Array[Byte],Array[Byte]]();

      nodes.foreach( tup => bulkmap.put( tup._1.toByteArray, toRLP( tup._2 ).toArray ) );
      kvds.updateBatch( bulkmap );
    }
    def commit() : Unit = ();
    def rollback() : Unit = ();
  }
}
class DirectEthereumJTrie private ( val innerDb : DirectEthereumJTrie.InnerDb, rootHash : Array[Byte], val secure : Boolean ) extends EthereumJTrie {

  import DirectEthereumJTrie.EthNode;

  def this( kvds : KeyValueDataSource, rootHash : Array[Byte], secure : Boolean, caching : Boolean ) = {
    this( if ( caching ) new DirectEthereumJTrie.CachingDb( kvds ) else new DirectEthereumJTrie.DirectDb( kvds ), rootHash, secure );
  }

  def this( kvds : KeyValueDataSource, secure : Boolean, caching : Boolean ) = this( kvds, EthTrieDb.EmptyHash.toByteArray, secure, caching );

  private def recreateInnerTrie( root : Array[Byte] ) : GenericEthTrie = {
    if ( secure ) new InnerSecureTrie( EthHash.withBytes(root) ) else new InnerTrie( EthHash.withBytes( root ) );
  }

  // MT: protected with this' lock
  private var innerTrie : GenericEthTrie = recreateInnerTrie( rootHash )

  // MT: protected with this' lock
  private var lastCheckpoint : GenericEthTrie = innerTrie;

  private def _k( a : Array[Byte] ) : IndexedSeq[Nibble] = toNibbles( a.toSeq );

  val caching : Boolean = innerDb.isInstanceOf[DirectEthereumJTrie.CachingDb];

  def get( key : Array[Byte] ) : Array[Byte] = this.synchronized { 
    innerTrie( _k( key ) ).fold( EthereumJTrie.DELETE_TOKEN )( _.toArray ) 
  }
  def update( key : Array[Byte], value : Array[Byte] ) : Unit = this.synchronized { 
    if ( value.isEmpty )
      delete( key )
    else
      innerTrie = innerTrie.including( _k( key ), value.toIndexedSeq );
  }
  def delete( key : Array[Byte] ) : Unit = this.synchronized {
    innerTrie = innerTrie.excluding( _k( key ) );
  }
  def getRootHash() : Array[Byte] = this.synchronized {
    innerTrie.RootHash.toByteArray;
  }
  def setRoot( newRootHash : Array[Byte] ) : Unit = this.synchronized {
    innerTrie = recreateInnerTrie( newRootHash );
  }
  def sync() : Unit = this.synchronized {
    innerDb.commit()
    lastCheckpoint = innerTrie;
  }
  def undo() : Unit = this.synchronized {
    innerDb.rollback()
    innerTrie = lastCheckpoint;
  }
  def getTrieDump() : String = this.synchronized {
    innerTrie.captureTrieDump
  }
  def validate() : Boolean = ???;

  def bulkUpdateDelete( pairs : Iterable[(Array[Byte],Array[Byte])] ) : Unit = this.synchronized {
    pairs.foreach { pair =>
      if ( pair._2 == EthereumJTrie.DELETE_TOKEN )
        this.delete( pair._1 )
      else
        this.update( pair._1, pair._2 )
    }
  }

  def copy() : EthereumJTrie = this.synchronized {
    new DirectEthereumJTrie( innerDb, innerTrie.RootHash.toByteArray, secure );
  }

  private trait GenericEthTrie {
    def apply( key : IndexedSeq[Nibble] )                        : Option[Seq[Byte]];
    def including( key : IndexedSeq[Nibble], value : Seq[Byte] ) : GenericEthTrie;
    def excluding( key : IndexedSeq[Nibble] )                    : GenericEthTrie;
    def RootHash                                                 : EthHash;
    def captureTrieDump                                          : String;
  }
  private class InnerTrie( root : EthHash ) extends AbstractEthTrie[InnerTrie]( innerDb, root ) with GenericEthTrie {
    def instantiateSuccessor( newRootHash : EthHash ) : InnerTrie =  new InnerTrie( newRootHash );
  }
  private class InnerSecureTrie( root : EthHash ) extends AbstractEthSecureTrie[InnerSecureTrie]( innerDb, root ) with GenericEthTrie {
    def instantiateSuccessor( newRootHash : EthHash ) : InnerSecureTrie =  new InnerSecureTrie( newRootHash );
  }
}
