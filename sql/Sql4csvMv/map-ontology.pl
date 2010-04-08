#!/usr/bin/perl
use Data::Dumper;

open P,    $ARGV[0] or die "Can't open P file $ARGV[0] - $!";
open PV,   $ARGV[1] or die "Can't open PV file $ARGV[1] - $!";
open APV,  $ARGV[2] or die "Can't open APV file $ARGV[2] - $!";
open APVO, $ARGV[3] or die "Can't open APVO file $ARGV[3] - $!";
open OT,   $ARGV[4] or die "Can't open OT file $ARGV[4] - $!";

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

# 7754    204778461       1       mock
my $APV = {};
my $APV1 = {};
my $APV2 = {};
while(<APV>) {
	chomp;

	my ($apvid, $aid, $pid, $pv) = split /\t/;
	$pv = trim($pv);
	$aid = trim($aid);
	$pid = trim($pid);
	$apvid = trim($apvid);

	my $pvid = $PV->{$pid}->{$pv};

	if((!defined $pid) || (!defined $pvid)) {
		warn "$pid not found or $pv not found!";
		next;
	} 

	$APV->{$pid}->{$pvid}->{$aid} = $apvid;
	$APV1->{$pv}->{$aid} = $apvid;
	push @{$APV2->{$aid}}, $apvid;
}

# 1862    575119145       sepsis  EFO_0001420     
my $O = {};
while(<OT>) {
	chomp;
	my ($otid, $oid, $acc, $term) = split /\t/;
	$O->{$oid}->{$acc} = $otid;
}

# 1       226011447       EFO_0000302     575119145       fetal brain     AE1__ASSAY_ORGANISMPART__DM
my $D = {};
while(<APVO>) {
	chomp;
	my ($apvoid, $aid, $acc, $oid, $pv, $p) = split /\t/;

	$pv = trim($pv);
	$p =~ /AE1__(ASSAY|SAMPLE)_(.*)__DM/;
	$p = lc($2);

	my $pid = $P->{$p};
	my $pvid = $PV->{$pid}->{$pv};
	my $apvid = $APV->{$pid}->{$pvid}->{$aid};
	my $otid = $O->{$oid}->{$acc};

	warn "$p not found" and next if !defined $pid;
	warn "$pv not found" and next if !defined $pvid;

	if(exists $APV->{$pid}->{$pvid} and !defined $apvid) {
	  warn "$aid not found for ($p,$pv) - maybe assay without array design?";
	  $apvid = $APV1->{$pv}->{$aid};
	  warn "*** Found $aid: $apvid for $pv" if defined $apvid;
	
	  if(!defined $apvid) {
    	    ($apvid) = @{$APV2->{$aid}};
	    warn "$aid not found at all!" and next if !defined $apvid;
	    warn "*** Found $aid - $apvid!";
          }
	}

	warn "$p/$pv/$aid not found" and next if !defined $apvid;
	warn "$acc not found" and next if !defined $otid;

	if(!exists $D->{$otid . $apvid}) {
		print join("\t", $apvoid, $otid, $apvid), "\n";
	}
	$D->{$otid . $apvid} = 1;
}

close OT;
close APV;
close APVO;
close PV;
close P;

sub trim($)
{
        my $string = shift;
        $string =~ s/^\s+//;
        $string =~ s/\s+$//;
        return $string;
}

