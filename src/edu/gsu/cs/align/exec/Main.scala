package edu.gsu.cs.align.exec

import edu.gsu.cs.align.io.{MSAWriter, FASTAParser, SAMParser}
import java.io.{FileWriter, File, FileNotFoundException}
import edu.gsu.cs.align.model.{InsertionsAligner, InsertionsHandler}
import org.biojava3.core.sequence.DNASequence
import org.biojava3.core.sequence.io.FastaWriterHelper
import collection.JavaConversions._


/**
 * Created with IntelliJ IDEA.
 * User: aartyomenko
 * Date: 6/27/13
 * Time: 12:39 PM
 * Main object to run alignment extension based on
 * aligned reads (SAM file) of fasta reads via
 * alignment by InDelFixer [Topfer et. al.]
 */
object Main {
  /**
   * Main entry point. Redirect user to InDelFixer interface
   * or handle its own if reads already aligned.
   * @param args
   *             list of program arguments
   */
  def main(args: Array[String]): Unit = {
    try {
      val (path_to_ref, path_to_sam) = parseArgs(args)
      initReadsAdnReference(path_to_ref, path_to_sam)
      //val n = reads.map(r => r.getAlignmentStart + r.getReadLength).max
      InsertionsHandler.buildInsertionTable(reads, ref.getLength + 1)
      val exRef = InsertionsHandler.getExtendedReference(ref.getSequenceAsString)
      //val path_to_ext_ref = path_to_ref + "_ext.fasta"
      //FASTAParser.writeAsFASTA(exRef, path_to_ext_ref)
      val ext_len = if (end == -1) exRef.length else end + exRef.length - ref.getLength
      InsertionsAligner.buildAndInitInsertionsTable(reads, ext_len + 1)
      println("Table built")
      InsertionsAligner.performInsertionsAlignment
      println("Insertions aligned")
      val e_r = reads.map(r => {
        val seq = InsertionsAligner.transformRead(r, ext_len, start)
        val record = new DNASequence(seq)
        record.setOriginalHeader(r.getReadName)
        record
      })
      //MSAWriter.writeExtendedReadsInInternalFormat(path_to_sam + "_ext.txt",e_r)
      val fl = getOutputDirPath + File.separator + "aligned_reads.fas"
      val fw = new FileWriter(fl, false)
      FastaWriterHelper.writeNucleotideSequence(new File(fl), e_r)
      fw.close()

    } catch {
      case e: FileNotFoundException => {
        System.err.println(e.getMessage)
      }
      case e: IndexOutOfBoundsException => {
        System.err.println(e.getMessage)
        e.printStackTrace
      }
      case e: Exception => e.printStackTrace
    }
  }

  private def parseArgs(args: Array[String]) = {
    val sam = args.indexOf(ALIGNED_READS_PARAMETER)
    val g = args.indexOf(REFERENCE_PARAMETER)
    val o = args.indexOf(OUTPUT_PARAMETER)
    val r = args.indexOf(INTERVAL_PARAMETER)
    if (o != -1 && args.length > o + 1 && !args(o+1).endsWith(File.separator)) {
      args(o+1) = args(o+1) + File.separator
    }
    if (o == -1) output_folder = new File(System.getProperty(USER_DIR))
    else output_folder = new File(args(o + 1))
    var path_to_sam = ""
    path_to_sam = if (sam == -1) runInDelFixer(args) else args(sam + 1)
    if (g == -1) {
      System.err.println("Reference file is not specified! Use -g <path_to_ref>")
      System.exit(-1)
    }
    if (r != -1) {
      val tmp = args(r+1).split("-")
      start = tmp(0).toInt
      end = tmp(1).toInt
    }
    val path_to_ref = args(g + 1)
    (path_to_ref, path_to_sam)
  }
}
