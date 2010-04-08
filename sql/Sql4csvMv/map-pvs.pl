#!/usr/bin/perl
use Data::Dumper;

open PV, $ARGV[0] or die "Can't open PV file $ARGV[0] - $!";
open TPV, $ARGV[1] or die "Can't open TPV file $ARGV[1] - $!";

my $PV = {};

while(<PV>) {
	chomp;
	my ($pvid, $pid, $pv) = split /\t/;

	$PV->{$pid}->{$pv} = $pvid;
}


while(<TPV>) {
	chomp;
	my ($tpvid, $tid, $pid, $pv) = split /\t/;
	my $pvid = $PV->{$pid}->{$pv};
	if(!defined $pvid) {
		warn "$pv not found!";
		$pvid = "MISSING";
		next;
	}

	print join ("\t", $tpvid, $tid, $pid, $pvid), "\n";
}

close TPV;
close PV;
