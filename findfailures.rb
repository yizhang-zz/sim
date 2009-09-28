puts "Usage: findfailures.rb <file> <node/cluster id>"
puts
file = File.new(ARGV[0], "r")
node = ARGV[1].to_i
while (line = file.gets)
    if (line =~ /sim.nodes.Node/)
       if (line =~ /T (\d+) N #{node} failure/)
         print $1+"* "
         elsif (line =~ /T (\d+) N #{node} success/)
         print $1+" "
       end
    elsif (line =~ /sim.nodes.BaseStation/)
      if (line =~ /T (\d+) N #{node} discovered failures \[ ((\d+ )+)\]/)
         print $2
      end
    elsif (line =~ /sim.nodes.Cluster/)
      if (line =~ /T (\d+) C #{node} success/)
         print $1+" "
        elsif (line =~ /T (\d+) C #{node} failure/)
        print $1+"* "
      end
    end
  end
