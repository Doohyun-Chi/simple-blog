package com.simpleblog.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** A window of {@code size} rows after the first {@code offset}; unlike a PageRequest the offset need not be a multiple of the size. */
public record OffsetPage(long offset, int size) implements Pageable {

	@Override
	public int getPageNumber() {
		return (int) (this.offset / this.size);
	}

	@Override
	public int getPageSize() {
		return this.size;
	}

	@Override
	public long getOffset() {
		return this.offset;
	}

	@Override
	public Sort getSort() {
		return Sort.unsorted();
	}

	@Override
	public Pageable next() {
		return new OffsetPage(this.offset + this.size, this.size);
	}

	@Override
	public Pageable previousOrFirst() {
		return new OffsetPage(Math.max(0, this.offset - this.size), this.size);
	}

	@Override
	public Pageable first() {
		return new OffsetPage(0, this.size);
	}

	@Override
	public Pageable withPage(int pageNumber) {
		return new OffsetPage((long) pageNumber * this.size, this.size);
	}

	@Override
	public boolean hasPrevious() {
		return this.offset > 0;
	}

}
