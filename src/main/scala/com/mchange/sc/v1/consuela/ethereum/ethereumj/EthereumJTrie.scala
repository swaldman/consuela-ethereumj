package com.mchange.sc.v1.consuela.ethereum.ethereumj;

import com.mchange.sc.v1.consuela.ethereum._;
import com.mchange.sc.v1.consuela.ethereum.trie._;
import com.mchange.sc.v1.consuela.trie._;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.trie.{Trie => EJTrie};

object EthereumJTrie {
  val DELETE_TOKEN = Array.empty[Byte];
}
trait EthereumJTrie extends EJTrie {
  def copy() : EthereumJTrie;
  def bulkUpdateDelete( pairs : Iterable[(Array[Byte],Array[Byte])] ) : Unit;

  override def equals( obj : Any ) : Boolean = {
    super.equals(obj) || (obj.isInstanceOf[EthereumJTrie] && (obj.asInstanceOf[EthereumJTrie].getRootHash.toSeq == this.getRootHash.toSeq))
  }
  override def hashCode() : Int = this.getRootHash.toSeq.hashCode()
}

