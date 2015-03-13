package com.mchange.sc.v1.consuela.ethereum.ethereumj;

import com.mchange.sc.v1.consuela.ethereum._;
import com.mchange.sc.v1.consuela.ethereum.trie._;
import com.mchange.sc.v1.consuela.trie._;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.trie.{Trie => EJTrie};

class EthereumJTrie( kvds : KeyValueDataSource, rootHash : EthHash, secure : Boolean ) extends EJTrie {

  def this( kvds : KeyValueDataSource, rootHashBytes : Array[Byte], secure : Boolean ) = this( kvds, EthHash.withBytes( rootHashBytes ), secure );
  def this( kvds : KeyValueDataSource, secure : Boolean )                              = this( kvds, EthTrieDb.EmptyHash, secure );

  private type EthNode = EmbeddableEthStylePMTrie.Node[Nibble,Seq[Byte],EthHash]
  //type InnerTrie = PMTrie[Nibble,Seq[Byte],EthHash]; 

  private val innerDb = new InnerDb;

  // MT: protected with this' lock
  private var innerTrie : GenericEthTrie = if ( secure ) new InnerSecureTrie( rootHash ) else new InnerTrie( rootHash );

  // MT: protected with this' lock
  private var lastCheckpoint : GenericEthTrie = innerTrie;

  private def _k( a : Array[Byte] ) : IndexedSeq[Nibble] = toNibbles( a.toSeq );

  def get( key : Array[Byte] ) : Array[Byte] = this.synchronized { 
    innerTrie( _k( key ) ).fold( null.asInstanceOf[Array[Byte]] )( _.toArray ) 
  }
  def update( key : Array[Byte], value : Array[Byte] ) : Unit = this.synchronized { 
    innerTrie = innerTrie.including( _k( key ), value.toIndexedSeq );
  }
  def delete( key : Array[Byte] ) : Unit = this.synchronized {
    innerTrie = innerTrie.excluding( _k( key ) );
  }
  def getRootHash() : Array[Byte] = this.synchronized {
    innerTrie.RootHash.toByteArray;
  }
  def setRoot( newRootHash : Array[Byte] ) : Unit = this.synchronized {
    innerTrie = new InnerTrie( EthHash.withBytes( newRootHash ) );
  }
  def sync() : Unit = this.synchronized {
    lastCheckpoint = innerTrie;
  }
  def undo() : Unit = this.synchronized {
    innerTrie = lastCheckpoint;
  }
  def getTrieDump() : String = this.synchronized {
    innerTrie.captureTrieDump
  }
  def validate() : Boolean = ???;

  private class InnerDb extends EthTrieDb with PMTrie.Database.BulkWriting[EthNode,EthHash] {
    def put( hash : EthHash, node : Node ) : Unit = kvds.put( hash.toByteArray, toRLP( node ).toArray );
    def apply( hash : EthHash ) : Node = {
      val nodeBytes = kvds.get( hash.toByteArray );
      fromRLP( nodeBytes )
    }
    def put( nodes : Map[EthHash,EthNode] ) : Unit = {
      // this is a dangerous construction, as Java byte[] is not a good hash key,
      // we are hashing by identity. but it should work here, and is required by
      // the org.ethereum.datasource.KeyValueDataSource
      val bulkmap = new java.util.HashMap[Array[Byte],Array[Byte]]();
      nodes.foreach( tup => bulkmap.put( tup._1.toByteArray, toRLP( tup._2 ).toArray ) );
      kvds.updateBatch( bulkmap );
    }
  }

  private trait GenericEthTrie {
    def apply( key : IndexedSeq[Nibble] )                        : Option[Seq[Byte]];
    def including( key : IndexedSeq[Nibble], value : Seq[Byte] ) : GenericEthTrie;
    def excluding( key : IndexedSeq[Nibble] )                    : GenericEthTrie;
    def RootHash                                                 : EthHash;
    def captureTrieDump                                          : String;
  }
  private class InnerTrie( newRootHash : EthHash ) extends AbstractEthTrie[InnerTrie]( innerDb, newRootHash ) with GenericEthTrie {
    def instantiateSuccessor( newRootHash : EthHash ) : InnerTrie =  new InnerTrie( newRootHash );
  }
  private class InnerSecureTrie( newRootHash : EthHash ) extends AbstractEthSecureTrie[InnerSecureTrie]( innerDb, newRootHash ) with GenericEthTrie {
    def instantiateSuccessor( newRootHash : EthHash ) : InnerSecureTrie =  new InnerSecureTrie( newRootHash );
  }
}
