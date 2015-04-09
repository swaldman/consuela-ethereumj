package com.mchange.sc.v1.consuela.ethereum.ethereumj;

import com.mchange.sc.v1.consuela.ethereum._;
import com.mchange.sc.v1.consuela.ethereum.trie._;
import com.mchange.sc.v1.consuela.trie._;
import org.ethereum.trie._;
import org.ethereum.trie.Trie;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.datasource.HashMapDB;

/**
 *  null values for KeyValueDataSources are acceptable: an in-memory test KeyValueDataSource should be supplied.
 */  
class EthereumJTrieFactory extends TrieFactory {
  private def goodDb( db : KeyValueDataSource ) : KeyValueDataSource  = if ( db == null ) new MemoryOnlyKeyValueDataSource else db;

  def createSimpleTrie( db : KeyValueDataSource, rootHash : Array[Byte] ) : Trie = new UnifiedEthereumJTrie ( goodDb( db ), rootHash, secure=false, caching=true );
  def createSecureTrie( db : KeyValueDataSource, rootHash : Array[Byte] ) : Trie = new UnifiedEthereumJTrie ( goodDb( db ), rootHash, secure=true, caching=true );
  def createFatTrie( securedb: KeyValueDataSource, insecuredb :  KeyValueDataSource, rootHash : Array[Byte]) : FatTrie  = {
    EthereumJFatTrie.create( goodDb( insecuredb ), goodDb( securedb ), rootHash, caching=true );
  }
}
