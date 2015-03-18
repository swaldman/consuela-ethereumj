package com.mchange.sc.v1.consuela.ethereum.ethereumj;

import com.mchange.sc.v1.consuela.ethereum._;
import com.mchange.sc.v1.consuela.ethereum.trie._;
import com.mchange.sc.v1.consuela.trie._;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.trie.{Trie => EJTrie};
import org.ethereum.trie.FatTrie;

object EthereumJFatTrie {
  def createDirect( insecureKvds : KeyValueDataSource, secureKvds : KeyValueDataSource, rootHash : Array[Byte] ) : EthereumJFatTrie = {
    val insecure = new DirectEthereumJTrie( insecureKvds, rootHash, false );
    val secure   = new DirectEthereumJTrie(   secureKvds, rootHash, true  );
    new EthereumJFatTrie( insecure, secure, rootHash )
  }
  def createCached( insecureKvds : KeyValueDataSource, secureKvds : KeyValueDataSource, rootHash : Array[Byte] ) : EthereumJFatTrie = {
    val insecure = new CacheWrapperEthereumJTrie( new DirectEthereumJTrie( insecureKvds, rootHash, false ) );
    val secure   = new CacheWrapperEthereumJTrie( new DirectEthereumJTrie(   secureKvds, rootHash, true  ) );
    new EthereumJFatTrie( insecure, secure, rootHash )
  }
}
class EthereumJFatTrie private ( insecure : EthereumJTrie, secure : EthereumJTrie, rootHash : Array[Byte] ) extends EthereumJTrie with FatTrie {

  this.setRoot( rootHash );

  def get( key : Array[Byte] ) : Array[Byte] = this.synchronized {
    insecure.get( key );
  }
  def update( key : Array[Byte], value : Array[Byte] ) : Unit = this.synchronized { 
    insecure.update( key, value );
    secure.update( key, value );
  }
  def delete( key : Array[Byte] ) : Unit = this.synchronized {
    insecure.delete( key );
    secure.delete( key );
  }

  // following EthereumJ's implementation, secure's hashes are normative
  def getRootHash() : Array[Byte] = this.synchronized {
    secure.getRootHash();
  }

  def setRoot( newRootHash : Array[Byte] ) : Unit = this.synchronized {
    secure.setRoot( newRootHash );
  }
  def getTrieDump() : String = this.synchronized {
    secure.getTrieDump();
  }
  def sync() : Unit = this.synchronized {
    insecure.sync();
    secure.sync();
  }
  def undo() : Unit = this.synchronized {
    insecure.undo();
    secure.undo();
  }
  def validate() : Boolean = ???;

  def bulkUpdateDelete( pairs : Iterable[(Array[Byte],Array[Byte])] ) : Unit = this.synchronized {
    insecure.bulkUpdateDelete( pairs );
    secure.bulkUpdateDelete( pairs );
  }
  def copy() : EthereumJTrie = this.synchronized {
    return new EthereumJFatTrie( insecure.copy(), secure.copy(), this.getRootHash() );
  }
  def getOrigTrie() : EJTrie = this.synchronized {
    val out = insecure.copy();
    out.setRoot( this.getRootHash() );
    out;
  }
  def getSecureTrie() : EJTrie = this.synchronized {
    secure.copy();
  }
}


