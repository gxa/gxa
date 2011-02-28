BEGIN { 
	comment = 0;
	desired = table;
	table = "";
	sql = "";
}
/\/\*/ { comment = 1 }
/^SELECT \* FROM A2_/ {
	name = $4;
	sub(/A2_/, "", name);
	table = name;
} 
!comment && table == desired { 
	if (!sql) {
		split($0, parts, /A2_/);
		sql = substr(parts[2], length(table) + 1);
	} else {
		sql = sql $0;
	}
}
/\;$/ && sql { 
	table = "";
	print sql;
	sql = 0;
}
/\*\// { comment = 0 }
