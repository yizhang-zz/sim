class Interval
  attr_reader :begin, :end
  attr_writer :begin, :end
  def initialize(a,b)
    @begin = a
    @end = b
  end
  def to_s
    @begin.to_s+"-"+@end.to_s
  end
end

class IntervalList
  def initialize
    @list = Array.new
  end

  def add(a,b)
    # [a,b] can only overlap with the last one
    if (@list.length > 0 and @list[-1].end >= a-1)
      @list[-1].end = b
    else
      @list.push(Interval.new(a,b))
    end
  end

  def to_s
    str = ""
    @list.each { |s| str += s.to_s+","}
    str
  end
end

if ARGV.length == 1
  file = File.new("bstrace.txt","r")
  node = ARGV[0].to_i
elsif ARGV.length == 2
  file = File.new(ARGV[0], "r")
  node = ARGV[1].to_i
end

il = IntervalList.new

while (line = file.gets)
  if (line =~ /sim.nodes.BaseStation - T (\d+) N #{node} known interval (\d+) to (\d+)/)
    il.add($2.to_i,$3.to_i)
  end
end

puts il.to_s
