package com.mchange.sc.v1.consuela.ethereum.ethereumj;

import com.mchange.sc.v1.consuela.ethereum._;
import com.mchange.sc.v1.consuela.ethereum.trie._;
import com.mchange.sc.v1.consuela.trie._;
import org.ethereum.datasource.KeyValueDataSource;

import scala.collection._;

// Note: Originally tried to implement this in terms of scala.collection.concurrent.TrieMap,
//       but with out an atomic read-and-clear for the whole map, could not implement sync
//       without race conditions.

class CacheWrapperEthereumJTrie( inner : EthereumJTrie ) extends EthereumJTrie {

  import EthereumJTrie.DELETE_TOKEN;

  val map : mutable.Map[mutable.WrappedArray[Byte], Array[Byte]] = mutable.HashMap.empty;

  private def _k( key : Array[Byte] ) : mutable.WrappedArray[Byte] = new mutable.WrappedArray.ofByte( key );

  def get( key : Array[Byte] ) : Array[Byte] = this.synchronized {
    val got = map.getOrElseUpdate( _k( key ), inner.get( key ) );
    if ( got == DELETE_TOKEN ) null else got;
  }
  def update( key : Array[Byte], value : Array[Byte] ) : Unit = this.synchronized {
    map += ( _k( key ) -> value );
  }
  def delete( key : Array[Byte] ) : Unit = update( key, DELETE_TOKEN );
  def getRootHash() : Array[Byte] = this.synchronized {
    if ( isDirty ) 
      throw new IllegalStateException("Cannot compute the root hash of a Trie with cached, uncommitted modifications.");
    else
      inner.getRootHash()
  }
  def setRoot( newRootHash : Array[Byte] ) : Unit = this.synchronized {
    if ( isDirty ) 
      throw new IllegalStateException("Cannot set the root hash of a Trie with cached, uncommitted modifications.");
    else
      inner.setRoot( newRootHash )
  }
  def sync() : Unit = this.synchronized {
    inner.bulkUpdateDelete( map.map( pair => (pair._1.toArray, pair._2 ) ) );
    inner.sync();
  }
  def undo() : Unit = this.synchronized {
    map.clear();
  }
  def getTrieDump() : String = this.synchronized {
    if ( isDirty ) 
      throw new IllegalStateException("Cannot get the structure of a Trie with cached, uncommitted modifications.");
    else
      inner.getTrieDump()
  }
  def validate() : Boolean = this.synchronized {
    if ( isDirty ) 
      throw new IllegalStateException("Cannot validate a Trie with cached, uncommitted modifications.");
    else
      inner.validate()
  }
  def bulkUpdateDelete( pairs : Iterable[(Array[Byte],Array[Byte])] ) : Unit = this.synchronized {
    map ++= pairs.map( pair => ( _k( pair._1 ), pair._2 ) )
  }
  def copy() : EthereumJTrie = this.synchronized {
    if ( this.isDirty )
      throw new IllegalStateException("We can't copy() an EthereumJTrie (CacheWrapperEthereumJTrie) with unsynced changes! Call sync() first!");
    else
      new CacheWrapperEthereumJTrie( inner.copy() );
  }
  def isDirty : Boolean = this.synchronized {
    !map.isEmpty
  }
}
