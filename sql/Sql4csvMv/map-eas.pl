#!/usr/bin/perl
use Data::Dumper;

open P, $ARGV[0] or die "Can't open P file $ARGV[0] - $!";
open PV, $ARGV[1] or die "Can't open PV file $ARGV[1] - $!";
open EA, $ARGV[2] or die "Can't open EA file $ARGV[2] - $!";

my $PV = {};

while(<PV>) {
	chomp;
	my ($pvid, $pid, $pv) = split /\t/;

	$PV->{$pid}->{$pv} = $pvid;
}

my $P = {};

while(<P>) {
	chomp;
	my ($pid, $p) = split /\t/, $_, 3;
	$P->{$p} = $pid;
}

# 1       188506905       159021045       organism        Macaca mulatta  5.34586240882511        .00174528386091107      .0151938110780284       .00726552734156376

while(<EA>) {
	chomp;

	my @ea = split /\t/;
	my $p  = lc(trim($ea[3]));
	my $pv = trim($ea[4]);

        next if $pv eq 'V1';

	my $pid = $P->{$p};
	my $pvid = $PV->{$pid}->{$pv};

	if((!defined $pid) || (!defined $pvid)) {
		warn "$p not found or $pv not found!";
		next;
	} 

	$ea[3] = $pid;
	$ea[4] = $pvid;

	print join ("\t", @ea), "\n";
}

close EA;
close PV;
close P;

sub trim($)
{
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}
