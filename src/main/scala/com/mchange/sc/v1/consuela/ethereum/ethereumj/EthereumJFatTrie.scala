package com.mchange.sc.v1.consuela.ethereum.ethereumj;

import com.mchange.sc.v1.consuela.ethereum._;
import com.mchange.sc.v1.consuela.ethereum.trie._;
import com.mchange.sc.v1.consuela.trie._;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.trie.{Trie => EJTrie};

class EthereumJFatTrie( insecureKvds : KeyValueDataSource, secureKvds : KeyValueDataSource, rootHash : EthHash ) extends EJTrie {
  private val insecure = new EthereumJTrie( insecureKvds, rootHash, false );
  private val secure   = new EthereumJTrie(   secureKvds, rootHash, true  );

  def get( key : Array[Byte] ) : Array[Byte] = insecure.get( key );
  def update( key : Array[Byte], value : Array[Byte] ) : Unit = { 
    insecure.update( key, value );
    secure.update( key, value );
  }
  def delete( key : Array[Byte] ) : Unit = {
    insecure.delete( key );
    secure.delete( key );
  }

  // following EthereumJ's implementation, secure's hashes are normative
  def getRootHash() : Array[Byte] = secure.getRootHash();

  def setRoot( newRootHash : Array[Byte] ) : Unit = secure.setRoot( newRootHash );

  def getTrieDump() : String = secure.getTrieDump();

  def sync() : Unit = {
    insecure.sync();
    secure.sync();
  }
  def undo() : Unit = {
    insecure.undo();
    secure.undo();
  }
  def validate() : Boolean = ???;

}


