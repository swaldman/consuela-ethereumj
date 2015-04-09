package com.mchange.sc.v1.consuela.ethereum.ethereumj;

import scala.collection._;
import scala.collection.JavaConverters._
import org.ethereum.datasource.KeyValueDataSource;

class MemoryOnlyKeyValueDataSource extends KeyValueDataSource {
  var name : String = null;
  val map : mutable.HashMap[Seq[Byte],Array[Byte]] = mutable.HashMap.empty[Seq[Byte],Array[Byte]];

  def init : Unit = ();
  def setName( name : String ) : Unit = this.synchronized {
    this.name = name;
  }
  def get( key : Array[Byte] ) : Array[Byte] = this.synchronized {
    map.getOrElse( key.toSeq, null );
  }
  def put( key : Array[Byte], value : Array[Byte] ) : Array[Byte] = this.synchronized {
    map += ( key.toSeq -> value );
    value
  }
  def delete( key : Array[Byte] ) : Unit = this.synchronized {
    map -= key.toSeq;
  }
  def keys() : java.util.Set[Array[Byte]] = this.synchronized {
    map.keySet.map( _.toArray ).asJava
  }
  def updateBatch(jMap: java.util.Map[Array[Byte],Array[Byte]]): Unit = this.synchronized {
    jMap.asScala.foreach( pair => this.put( pair._1, pair._2 ) );
  }

  def close(): Unit = ()

}
